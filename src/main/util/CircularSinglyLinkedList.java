/**
 * Copyright 2010 Rob Jansen
 * 
 * This file is part of braids-tor-simulator.
 * 
 * braids-tor-simulator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * braids-tor-simulator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with braids-tor-simulator.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * $Id: CircularSinglyLinkedList.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.util;

/**
 * A singly linked list whose last element points to the first. The element
 * stored in the list must contain a getId method. The IDs can be used to order
 * the element sequentially in the list by using the addInOrder method.
 * Otherwise ordering is not preserved. A CURRENT pointer to a single element in
 * the list is maintained and can be advanced in one direction around the list.
 * 
 * @author Rob Jansen
 * 
 * @param <T>
 *            the element to store in the list
 */
public class CircularSinglyLinkedList<T extends Identifiable> {
	/**
	 * The generic inner class to hold the actual elements stored in the list.
	 * The items allow each element to be linked to the next in the list.
	 * 
	 * @author Rob Jansen
	 * 
	 * @param <T>
	 *            the element to hold in this spot in the list
	 */
	static class Item<T> {
		/**
		 * The element this item represents
		 */
		T element;

		/**
		 * The element following this one in the linked list
		 */
		Item<T> next;

		/**
		 * Create a new list item that stores the given element
		 * 
		 * @param e
		 *            the element to store in this item
		 */
		public Item(T e) {
			element = e;
		}
	}

	/**
	 * The CURRENT pointer to an element in the list
	 */
	private Item<T> currentItem;

	/**
	 * The number of items in this list
	 */
	private int size;

	/**
	 * Adds the given element to the list immediately following the position of
	 * the CURRENT pointer. If the list is empty, the CURRENT pointer points to
	 * the given element. Ordering is not preserved with this method.
	 * 
	 * @param e
	 *            the element to add
	 */
	public void add(T e) {
		size++;

		if (currentItem == null) {
			currentItem = new Item<T>(e);
			currentItem.next = currentItem;
		} else {
			Item<T> temp = currentItem.next;
			currentItem.next = new Item<T>(e);
			currentItem = currentItem.next;
			currentItem.next = temp;
		}
	}

	/**
	 * Adds the given element to the list in order. Ordering is preserved by
	 * calling the getId() method to find the correct insertion point for the
	 * element. If ordering is not important, the add() method should be used as
	 * it is much more efficient.
	 * 
	 * @param e
	 *            the element to add to the list
	 */
	public void addInOrder(T e) {
		size++;

		if (currentItem == null) {
			currentItem = new Item<T>(e);
			currentItem.next = currentItem;
		} else {
			int id = e.getId();
			int start = currentItem.element.getId();
			if ((id > start) && (id < currentItem.next.element.getId())) {
				placeNext(e, currentItem);
			} else if (id < start) {
				Item<T> item = currentItem;
				while (item.next.element.getId() > start) {
					item = item.next;
				}
				if (id < item.next.element.getId()) {
					placeNext(e, item);
				} else {
					while ((id > item.next.element.getId())
							&& (item.next != currentItem)) {
						item = item.next;
					}
					placeNext(e, item);
				}
			} else {
				Item<T> item = currentItem;
				while ((id > item.next.element.getId())
						&& (item.next != currentItem)) {
					item = item.next;
				}
				placeNext(e, item);
			}

		}
	}

	/**
	 * Advances the CURRENT pointer one position and return the element stored
	 * at the new position.
	 * 
	 * @return null if there is no CURRENT item, i.e. the list is empty,
	 *         otherwise the next item in the list
	 */
	public T advance() {
		if (currentItem == null) {
			return null;
		}
		currentItem = currentItem.next;
		return currentItem.element;
	}

	/**
	 * Requests the element immediately following the given element in the list.
	 * The search loops around the list until the given element is found or a
	 * complete traversal occurs, whichever happens first. The CURRENT pointer
	 * is unchanged.
	 * 
	 * @param e
	 *            the element preceding the requested element
	 * @return the element following the given element, or null in case the list
	 *         is empty or the given element is not in the list
	 */
	public T next(T e) {
		if (currentItem == null) {
			return null;
		}

		if (currentItem.element == e) {
			return currentItem.next.element;
		} else {
			Item<T> item = currentItem;
			while (item.next != currentItem) {
				if (item.next.element == e) {
					return item.next.next.element;
				}
				item = item.next;
			}
		}
		return null;
	}

	/**
	 * @return the element at the CURRENT pointer in the list, or null in case
	 *         the list is empty.
	 */
	public T current() {
		if (currentItem == null) {
			return null;
		}
		return currentItem.element;
	}

	/**
	 * @return true if the size of the list is 0
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Creates a new item from the given element and splices it after the given
	 * item.
	 * 
	 * @param e
	 *            the new element to insert
	 * @param item
	 *            the item whose next element will be set to the given element
	 */
	private void placeNext(T e, Item<T> item) {
		Item<T> temp = item.next;
		item.next = new Item<T>(e);
		item = item.next;
		item.next = temp;
	}

	/**
	 * Removes the given item from the list, if it exists. At most a complete
	 * traversal of the list will occur in case the item does not exist,
	 * otherwise the traversal stops after the item is found. The CURRENT
	 * pointer is unchanged unless it points to the item to remove, in which
	 * case it will be advanced to the following element in the list or be null
	 * if the resulting list is empty.
	 * 
	 * @param e
	 *            the element to remove from the list
	 */
	public void remove(T e) {
		if (currentItem == null) {
			return;
		}

		if (currentItem.element == e) {
			size--;
			if (currentItem.next == currentItem) {
				currentItem = null;
				return;
			} else {
				Item<T> item = currentItem;
				while (item.next != currentItem) {
					item = item.next;
				}
				item.next = currentItem.next;
				currentItem = currentItem.next;
			}
		} else {
			Item<T> item = currentItem;
			while (item.next != currentItem) {
				if (item.next.element == e) {
					size--;
					item.next = item.next.next;
					return;
				}
				item = item.next;
			}
		}
	}

	/**
	 * @return the number of elements stored in the list
	 */
	public int size() {
		return size;
	}
}