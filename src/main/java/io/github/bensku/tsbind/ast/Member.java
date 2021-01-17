package io.github.bensku.tsbind.ast;

import java.util.Optional;

public abstract class Member implements AstNode {

	/**
	 * Javadoc of this member, if it exists.
	 */
	public Optional<String> javadoc;
	
	/**
	 * Whether or not this member is public. Note that in some cases e.g.
	 * interface members are implicitly public.
	 */
	public final boolean isPublic;
	
	/**
	 * Whether or not this member is static.
	 */
	public final boolean isStatic;
	
	public Member(String javadoc, boolean isPublic, boolean isStatic) {
		this.javadoc = Optional.ofNullable(javadoc);
		this.isPublic = isPublic;
		this.isStatic = isStatic;
	}
	
	public abstract String name();
	
}
