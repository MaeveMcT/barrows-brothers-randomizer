package com.barrowsthemes;

public enum NpcAnimationMode
{
	NATIVE("Native only"), AUTO("Auto (conservative)"), FORCE("Force brother actions");
	private final String label;
	NpcAnimationMode(String label) { this.label = label; }
	@Override public String toString() { return label; }
}
