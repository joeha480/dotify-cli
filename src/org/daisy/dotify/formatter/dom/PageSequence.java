package org.daisy.dotify.formatter.dom;


/**
 * Provides an interface for a sequence of pages.
 * 
 * @author Joel Håkansson
 */
public interface PageSequence extends Sequence {
	
	public Iterable<? extends Page> getPages();
	/**
	 * Gets the number of pages in this sequence
	 * @return returns the number of pages in this sequence
	 */
	public int getPageCount();
	/**
	 * Gets the page with the specified index, where index >= 0 && index < getPageCount()
	 * @param index the page index
	 * @return returns the page index
	 * @throws IndexOutOfBoundsException if index < 0 || index >= getPageCount()
	 */
	//public Page getPage(int index);
	/**
	 * Gets the page number offset for this page sequence
	 * @return returns the page number offset
	 */
	//public int getPageNumberOffset();
	
	//public FormatterFactory getFormatterFactory();
	
	//public Formatter getFormatter();
}