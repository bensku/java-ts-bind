package io.github.bensku.tsbind;

import java.util.stream.Stream;

import io.github.bensku.tsbind.ast.TypeDefinition;

/**
 * Consumes AST to produce e.g. type declarations.
 *
 * @param <T> Result type of this transformer.
 * If this is saved to disk, consider using {@link String} here.
 */
public interface AstConsumer<T> {
	
	class Result<T> {
		/**
		 * Name of this result.
		 */
		public final String name;

		/**
		 * Result of AST transformation.
		 */
		public final T result;

		protected Result(String name, T result) {
			this.name = name;
			this.result = result;
		}

	}

	/**
	 * Consumes types from given stream to produce zero or more results.
	 * @param types Stream of types.
	 * @return Stream of results.
	 */
	Stream<Result<T>> consume(Stream<TypeDefinition> types);
}
