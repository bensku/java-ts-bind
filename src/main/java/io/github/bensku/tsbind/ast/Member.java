package io.github.bensku.tsbind.ast;

import java.util.Optional;

public abstract class Member {

	/**
	 * Javadoc of this member, if it exists.
	 */
	public final Optional<String> javadoc;
	
	/**
	 * Whether or not this member is static.
	 */
	public final boolean isStatic;
	
	public Member(String javadoc, boolean isStatic) {
		this.javadoc = Optional.ofNullable(javadoc);
		this.isStatic = isStatic;
	}
	
}