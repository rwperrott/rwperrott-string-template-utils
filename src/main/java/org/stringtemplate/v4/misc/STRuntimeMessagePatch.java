package org.stringtemplate.v4.misc;

/**
 * Converts relative template line numbers from STRuntimeMessages, to absolute line numbers.
 *
 * @author rwperrott
 */
public class STRuntimeMessagePatch extends STRuntimeMessage {
  final int absoluteStartLineNumber;

  public STRuntimeMessagePatch(final STRuntimeMessage of, final int absoluteStartLineNumber) {
    super(of.interp, of.error, of.ip, of.scope, of.cause, of.arg, of.arg2, of.arg3);
    this.absoluteStartLineNumber = absoluteStartLineNumber;
  }

  @Override
  public String getSourceLocation() {
    if (ip < 0 || self == null || self.impl == null) return null;
    final Interval interval = self.impl.sourceMap[ip];
    if (interval == null) return null;
    // Count line and charPos to I.a position
    final String s = self.impl.template;
    int p = 0;
    int index = interval.a;
    int line = absoluteStartLineNumber;
    int charPos = 0;
    while (p < index) {
      switch (s.charAt(p++)) {
        case '\r':
          if (p < index && s.charAt(p) == '\n')
            p++;
        case '\n':
          charPos = 0;
          line++;
          break;
        default:
          charPos++;
          break;
      }
    }

    return new Coordinate(line, charPos).toString();
  }
}
