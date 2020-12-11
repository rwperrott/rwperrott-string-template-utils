# string-template-utils
General utilities for use with [StringTemplate](http://www.stringtemplate.org/) v4 jar, with utilities for my rebuild of string-template-maven-plugin

The core is allows far more powerful MethodHandle based replacements for the original Method based ObjectModelAdapter
and StringRenderer, the ability to use parameterised methods in both the model class and compatible static methods in
other class, thus allows easy bypass of the stupid no-null and boolean conditional restrictions of the StringTemplate
interpreter.

Functionality includes:
- A TypeConverter class, which make matching/conversion of property values to method parameter types easy.
- A MemberInvoker class, which makes matching and invoking Members easy, with MethodHandles for much faster invoke.
- A MethodInvokers class, which can be fast searched for both instance and static MemberInvoker objects.
- A TypeFunctions class, which allow registration, for a type, of instance fields and methods, and static methods
  methods, and request of a MethodInvokers object for each a Member name(s).
- An ObjectFunctions class, designed to be registered with TypeFunction, providing some useful Object static methods.
- A StringFunctions class, designed to be registered with TypeFunction, providing some useful String static methods.
- An AbstractInvokeAdapter class, which extends ModelAdapter providing an abstract base for calling fields,
  and parameterised Method, with parameters in chained properties, for a type.  Parameters are joined/matched using a
  hidden Composite Object driven by it's own hidden ModelAdapter, or a toString() call.  Chaining different types is
  possible, via lookup of the appropriate ModelHandler from the used STGroup.
  in the same property chain, via embedded ModelAdapter lookup.
- A StringInvokeAdapter class, which extends InvokeAdapter for String type use.
- A StringInvokeRender class, which extends AttributeRenderer and uses TypeFunctions to access instance String methods
  with no parameters and static methods accepting a String parameter, and possibly a Locale parameter.
- Most of the classes are public for directly use or protected so that other libraries can extend them.

The code was designed to be Thread-safe too, with minimal locking.

### TODO
- Consider adding more to the Javadocs or provide examples in Markdown files.
