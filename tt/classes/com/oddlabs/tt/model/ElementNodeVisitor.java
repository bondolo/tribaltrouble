package com.oddlabs.tt.model;

public strictfp interface ElementNodeVisitor<E> {
	void visitNode(ElementNode<E> node);
	void visitLeaf(ElementLeaf<E> leaf);
	void visit(Element<E> element);
}
