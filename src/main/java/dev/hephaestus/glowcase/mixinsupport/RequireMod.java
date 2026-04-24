package dev.hephaestus.glowcase.mixinsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface RequireMod {
	/// The required modId
	String value();
	/// Whenever the mod should be present or absent
	boolean present() default true;
}
