package io.github.bensku.tsbind.binding;

public class TsField extends TsMember {

	/**
	 * Type of this field.
	 */
	private final TsType type;
	
	protected TsField(String name, TsType type) {
		super(name);
		this.type = type;
	}

	@Override
	public void emit(TsEmitter out) {
		out.println("%s: %s;", name, type);
	}

}
