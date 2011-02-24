/*
 * Copyright 1999-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * $Id: LogTableComparatorChooser.java,v 1.1 2005/10/06 12:54:42 jpassenger Exp $
 */
package org.logview4j.ui.table;

import java.awt.AWTEventMulticaster;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.impl.SortIconFactory;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;

/**
 * A TableComparatorChooser is a tool that allows the user to sort a ListTable by clicking
 * on the table's headers. It requires that the ListTable has a SortedList as
 * a source as the sorting on that list is used.
 *
 * <p>The TableComparatorChooser includes custom arrow icons that indicate the sort
 * order. The icons used are chosen based on the current Swing look and feel.
 * Icons are available for the following look and feels: Mac OS X, Metal, Windows.
 *
 * <p>The TableComparatorChooser supports multiple sort strategies for each
 * column, specified by having muliple comparators for each column. This may
 * be useful when you want to sort a single column in either of two ways. For
 * example, when sorting movie names, "The Phantom Menace" may be sorted under
 * "T" for "The", or "P" for "Phantom".
 *
 * <p>The TableComparatorChooser supports sorting multiple columns simultaneously.
 * In this mode, the user clicks a first column to sort by, and then the user
 * clicks subsequent columns. The list is sorted by the first column and ties
 * are broken by the second column.
 *
 * @see <a href="https://glazedlists.dev.java.net/issues/show_bug.cgi?id=4">Bug 4</a>
 * @see <a href="https://glazedlists.dev.java.net/issues/show_bug.cgi?id=31">Bug 31</a>
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 * 
 * Mine adds the ability not to sort on certain columns, as everything is knobbled in the
 * super I had to grab a copy and hack it
 */
public class LogTableComparatorChooser extends AbstractTableComparatorChooser {

	/**
	 * the header renderer which decorates an underlying renderer
	 * (the table header's default renderer) with a sort arrow icon.
	 */
	private final SortArrowHeaderRenderer sortArrowHeaderRenderer;
	/** listen for table and mouse events */
	private final Listener listener = new Listener();
	/** the table being sorted */
	private JTable table = null;
	/** listeners to sort change events */
	private ActionListener sortListener = null;
	/** the sort icons to use */
	private static Icon[] icons = SortIconFactory.loadIcons();
	protected final boolean[] sortedColumns;

	/**
	 * Creates a new TableComparatorChooser that responds to clicks
	 * on the specified table and uses them to sort the specified list.
	 *
	 * @param table the table with headers that can be clicked on.
	 * @param sortedList the sorted list to update.
	 * @param multipleColumnSort <code>true</code> to sort by multiple columns
	 *      at a time, or <code>false</code> to sort by a single column. Although
	 *      sorting by multiple columns is more powerful, the user interface is
	 *      not as simple and this strategy should only be used where necessary.
	 */
	public LogTableComparatorChooser(JTable table, SortedList sortedList,
			boolean multipleColumnSort, int[] sortedColumns, int defaultSortColumn) {
		super(sortedList, ((EventTableModel) table.getModel()).getTableFormat(),
				multipleColumnSort);

		// save the Swing-specific state
		this.table = table;

		// build and set the table header renderer which decorates the existing renderer with sort arrows
		sortArrowHeaderRenderer = new SortArrowHeaderRenderer();
		table.getTableHeader().setDefaultRenderer(sortArrowHeaderRenderer);

		// listen for events on the specified table
		table.setColumnSelectionAllowed(false);
		table.getTableHeader().addMouseListener(listener);
		table.getModel().addTableModelListener(listener);
		this.sortedColumns = indexSortingColumns(sortedColumns);

		columnClicked(defaultSortColumn, 1);
		columnClicked(defaultSortColumn, 1);
	}

	/**
	 * Registers the specified {@link ActionListener} to receive notification whenever
	 * the {@link JTable} is sorted by this {@link TableComparatorChooser}.
	 */
	public void addSortActionListener(final ActionListener sortActionListener) {
		sortListener = AWTEventMulticaster.add(sortListener, sortActionListener);
	}

	/**
	 * Deregisters the specified {@link ActionListener} to no longer receive
	 * action events.
	 */
	public void removeSortActionListener(final ActionListener sortActionListener) {
		sortListener = AWTEventMulticaster.remove(sortListener, sortActionListener);
	}

	/**
	 * Examines the current {@link Comparator} of the SortedList and
	 * adds icons to the table header renderers in response.
	 *
	 * <p>To do this, clicks are injected into each of the
	 * corresponding <code>ColumnClickTracker</code>s.
	 */
	protected void redetectComparator(Comparator currentComparator) {
		super.redetectComparator(currentComparator);

		// force the table header to redraw itself
		table.getTableHeader().revalidate();
		table.getTableHeader().repaint();
	}

	/**
	 * Updates the comparator in use and applies it to the table.
	 */
	protected final void rebuildComparator() {
		super.rebuildComparator();

		// force the table header to redraw itself
		table.getTableHeader().revalidate();
		table.getTableHeader().repaint();

		// notify interested listeners that the sorting has changed
		if (sortListener != null) {
			sortListener.actionPerformed(new ActionEvent(this, 0, "sort"));
		}
	}

	/**
	 * Gets the sorting style currently applied to the specified column.
	 */
	protected final int getSortingStyle(int column) {
		return super.getSortingStyle(table.convertColumnIndexToModel(column));
	}

	/**
	 * Determines if the specified mouse event shall be handled by this
	 * {@link TableComparatorChooser}. The default implementation handles only clicks
	 * with the left mouse button. Extending classes can customize which mouse
	 * events the table comparator chooser responds to by overriding this method.
	 */
	protected boolean isSortingMouseEvent(MouseEvent e) {
		return (e.getButton() == MouseEvent.BUTTON1);
	}

	/**
	 * Indexes the sorting columns
	 * @param columns the column indexes that are sorting
	 * @return the sorted columns index
	 */
	private boolean[] indexSortingColumns(int[] columns) {
		boolean[] index = new boolean[table.getColumnCount()];

		/**
		 * Mark the column as case insensitive if
		 * requested
		 */
		for (int i = 0; i < columns.length; i++) {
			if (columns[i] > -1 && columns[i] < index.length) {
				index[columns[i]] = true;
			}
		}

		return index;
	}

	/**
	 * Nested Listener class handles table events and mouse events.
	 */
	private class Listener extends MouseAdapter implements TableModelListener {

		/**
		 * When the mouse is clicked, this selects the next comparator in
		 * sequence for the specified table. This will re-sort the table
		 * by a new criterea.
		 *
		 * This code is based on the Java Tutorial's TableSorter
		 * @see <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/table.html#sorting">The Java Tutorial</a>
		 */
		public void mouseClicked(MouseEvent e) {
			if (!isSortingMouseEvent(e)) {
				return;
			}

			TableColumnModel columnModel = table.getColumnModel();
			int viewColumn = columnModel.getColumnIndexAtX(e.getX());
			int column = table.convertColumnIndexToModel(viewColumn);
			int clicks = e.getClickCount();

			if (clicks >= 1 && column != -1 && sortedColumns[column]) {
				columnClicked(column, clicks);
			}
		}

		/**
		 * When the number of columns changes in the table, we need to
		 * clear the comparators and columns.
		 */
		public void tableChanged(TableModelEvent event) {
			if (event.getFirstRow() == TableModelEvent.HEADER_ROW
					&& event.getColumn() == TableModelEvent.ALL_COLUMNS) {
				// the table structure may have changed due to a change in the
				// table format so we conservatively reset the TableFormat
				setTableFormat(((EventTableModel) table.getModel()).getTableFormat());
			}

			// if the comparator has changed
			Comparator currentComparator = sortedList.getComparator();
			if (currentComparator != sortedListComparator) {
				redetectComparator(currentComparator);
			}
		}
	}

	/**
	 * Releases the resources consumed by this {@link TableComparatorChooser} so that it
	 * may eventually be garbage collected.
	 *
	 * <p>A {@link TableComparatorChooser} will be garbage collected without a call to
	 * {@link #dispose()}, but not before its source {@link EventList} is garbage
	 * collected. By calling {@link #dispose()}, you allow the {@link TableComparatorChooser}
	 * to be garbage collected before its source {@link EventList}. This is
	 * necessary for situations where an {@link TableComparatorChooser} is short-lived but
	 * its source {@link EventList} is long-lived.
	 *
	 * <p><strong><font color="#FF0000">Warning:</font></strong> It is an error
	 * to call any method on a {@link TableComparatorChooser} after it has been disposed.
	 */
	public void dispose() {
		super.dispose();

		// if the default renderer within the table header is our sort arrow renderer,
		// uninstall it by restoring the table header's original default renderer
		if (table.getTableHeader().getDefaultRenderer() == sortArrowHeaderRenderer) {
			table.getTableHeader().setDefaultRenderer(
					sortArrowHeaderRenderer.getDelegateRenderer());
		}

		// remove our listeners from the table's header and model
		table.getTableHeader().removeMouseListener(listener);
		table.getModel().removeTableModelListener(listener);

		// null out our table reference for safety's sake
		table = null;
	}

	/**
	 * Set all {@link TableComparatorChooser}s to use the icons from the directory
	 * specified. The folder should contain the following eight icon files:
	 * <li>primary_sorted.png                      <li>secondary_sorted.png
	 * <li>primary_sorted_alternate.png            <li>secondary_sorted_alternate.png
	 * <li>primary_sorted_alternate_reverse.png    <li>secondary_sorted_alternate_reverse.png
	 * <li>primary_sorted_reverse.png              <li>secondary_sorted_reverse.png
	 *
	 * <p>Note that this path must be on the system classpath. It may be within a
	 * jar file.
	 */
	public static void setIconPath(String path) {
		icons = SortIconFactory.loadIcons(path);
	}

	/**
	 * The SortArrowHeaderRenderer simply delegates most of the rendering
	 * to the previous renderer, and adds an icon to indicate sorting
	 * direction. This eliminates the hassle of setting the border and
	 * background colours.
	 *
	 * <p>This class fails to add indicator arrows on tables where the
	 * renderer does not extend DefaultTableCellRenderer.
	 */
	class SortArrowHeaderRenderer implements TableCellRenderer {

		/** the renderer to delegate */
		private TableCellRenderer delegateRenderer;

		/**
		 * Creates a new SortArrowHeaderRenderer that delegates most drawing
		 * to the tables current header renderer.
		 */
		public SortArrowHeaderRenderer() {
			// find the delegate
			this.delegateRenderer = table.getTableHeader().getDefaultRenderer();
		}

		/**
		 * Returns the delegate renderer that is decorated with sort arrows.
		 */
		public TableCellRenderer getDelegateRenderer() {
			return this.delegateRenderer;
		}

		/**
		 * Renders the header in the default way but with the addition of an icon.
		 */
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			final Component rendered = delegateRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus,
					row, column);

			if (rendered instanceof JLabel) {
				final JLabel label = (JLabel) rendered;
				final Icon iconToUse = icons[getSortingStyle(column)];
				label.setIcon(iconToUse);
				label.setHorizontalTextPosition(SwingConstants.LEADING);
			}

			return rendered;
		}
	}
}
