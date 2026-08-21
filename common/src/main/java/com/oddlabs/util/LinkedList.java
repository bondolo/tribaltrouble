package com.oddlabs.util;

import org.jspecify.annotations.Nullable;

/** Hierarchical linked-list */
public final class LinkedList<T extends ListElement<T>> {
    private @Nullable T first;
    private @Nullable T last;
    private int size = 0;

    /**
     * {@return true if element is already in list otherwise false}
     */
    private boolean checkOwner(T elem) {
        if (elem.getListOwner() == this) return true;
        if (elem.getListOwner() != null) {
            elem.getListOwner().remove(elem);
        }
        elem.setListOwner(this);
        return false;
    }

    public void addLast(T elem) {
        if (checkOwner(elem)) return;
        if (last == null) {
            first = elem;
            last = elem;
            elem.setNext(null);
            elem.setPrior(null);
        } else {
            elem.setNext(null);
            elem.setPrior(last);
            last.setNext(elem);
            last = elem;
        }
        size++;
    }

    public void addFirst(T elem) {
        if (checkOwner(elem)) return;
        if (last == null) {
            first = elem;
            last = elem;
            elem.setNext(null);
            elem.setPrior(null);
        } else {
            elem.setNext(first);
            elem.setPrior(null);
            first.setPrior(elem);
            first = elem;
        }
        size++;
    }

    public void remove(T element) {
        assert element.getListOwner() == this;
        element.setListOwner(null);
        if (last == element && first == element) {
            first = null;
            last = null;
        } else if (last == element) {
            last = element.getPrior();
            last.setNext(null);
        } else if (first == element) {
            first = element.getNext();
            first.setPrior(null);
        } else {
            element.getPrior().setNext(element.getNext());
            element.getNext().setPrior(element.getPrior());
        }
        size--;
    }

    public void insert(T element, @Nullable T next_elem) {
        if (next_elem == null) {
            addLast(element);
            return;
        }
        checkOwner(element);
        assert next_elem.getListOwner() == this : "owner " + next_elem.getListOwner() + " != " + this;
        if (first == next_elem) {
            first = element;
            element.setPrior(null);
        } else {
            T prev = next_elem.getPrior();
            element.setPrior(prev);
            prev.setNext(element);
        }
        next_elem.setPrior(element);
        element.setNext(next_elem);
        size++;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public @Nullable T getFirst() {
        return first;
    }

    public @Nullable T getLast() {
        return last;
    }

    public void putLast(T element) {
        remove(element);
        addLast(element);
    }

    public void putFirst(T element) {
        remove(element);
        addFirst(element);
    }
}
