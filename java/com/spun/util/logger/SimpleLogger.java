package com.spun.util.logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.spun.util.DateDifference;
import com.spun.util.ObjectUtils;
import com.spun.util.ThreadUtils;

public class SimpleLogger
{
  public static class Symbols
  {
    public static String       markerIn  = "=> ";
    public static String       markerOut = "<= ";
    public static final String event     = "Event: ";
    public static String       variable  = "Variable: ";
    public static String       sql       = "Sql: ";
  }
  public static final int   IN            = 1;
  public static final int   OUT           = -1;
  public static boolean     marker        = true;
  public static boolean     event         = true;
  public static boolean     variable      = true;
  public static boolean     query         = true;
  public static boolean     timestamp     = true;
  public static boolean     stacktraces   = true;
  public static int         hourGlass     = 0;
  public static int         hourGlassWrap = 100;
  private static int        m_indent      = 0;
  private static long       lastTime      = System.currentTimeMillis();
  private static Appendable logTo         = System.out;
  public static void toggleAll(boolean t)
  {
    marker = t;
    event = t;
    variable = t;
    query = t;
  }
  private static void clearHourGlass()
  {
    if (hourGlass > 0)
    {
      logLine("");
      hourGlass = 0;
    }
  }
  public static void setHourGlassWrap(int numberOfDots)
  {
    hourGlassWrap = numberOfDots;
  }
  public static void hourGlass()
  {
    if (hourGlassWrap <= hourGlass)
    {
      clearHourGlass();
    }
    if (hourGlass == 0)
    {
      log(timeStampTextOnly());
    }
    hourGlass++;
    String mark = ((hourGlass % 10) == 0) ? ("" + (hourGlass / 10)) : ".";
    log(mark);
  }
  public static long startTimer()
  {
    return System.currentTimeMillis();
  }
  public static void stopTimer(long startTime, long maxTime, String function)
  {
    long diff = (System.currentTimeMillis() - startTime);
    if (diff > maxTime)
    {
      warning("Time Limit Exceeded - " + function + " [" + new DateDifference(diff).getStandardTimeText(2) + " > "
          + maxTime + "]");
    }
  }
  public static void markerIn(String statement)
  {
    if (!marker) { return; }
    logLine(timeStamp() + Symbols.markerIn + statement + " - IN");
    m_indent++;
  }
  private static String extractMarkerText()
  {
    try
    {
      StackTraceElement trace[] = ThreadUtils.getStackTrace();
      StackTraceElement element = trace[4];
      String className = element.getClassName();
      className = className.substring(className.lastIndexOf(".") + 1);
      return className + "." + element.getMethodName() + "()";
    }
    catch (Throwable t)
    {
      return "Can't Inspect Stack Trace";
    }
  }
  private static String getIndent()
  {
    if (m_indent == 0) { return ""; }
    String theIndention = "";
    for (int i = 0; i < m_indent; i++)
    {
      theIndention += "   ";
    }
    return theIndention;
  }
  private static String timeStamp()
  {
    clearHourGlass();
    return timeStampTextOnly();
  }
  private static String timeStampTextOnly()
  {
    String text = "";
    long current = System.currentTimeMillis();
    if (timestamp)
    {
      java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance();
      text = "[" + df.format(new java.util.Date(current)) + " ~" + padNumber(current - lastTime) + "ms] ";
    }
    text += getIndent();
    lastTime = current;
    return text;
  }
  private static String padNumber(long number)
  {
    String text = "" + number;
    while (text.length() < 6)
    {
      text = "0" + text;
    }
    return text;
  }
  private static String indentMessage(String message)
  {
    Vector<Integer> v = new Vector<Integer>();
    int place = 0;
    while ((place = message.indexOf('\n', place + 1)) != -1)
    {
      v.addElement(place);
    }
    if (v.size() == 0)
    {
      // no '\n'
      return message;
    }
    String theIndention = getIndent();
    StringBuffer buffer = new StringBuffer(message);
    for (int i = (v.size() - 1); i >= 0; i--)
    {
      int tempplace = ((Integer) v.elementAt(i)).intValue();
      buffer.insert(tempplace + 1, theIndention);
    }
    return buffer.toString();
  }
  public synchronized static void markerOut(String text)
  {
    if (!marker) { return; }
    m_indent--;
    logLine(timeStamp() + Symbols.markerOut + text + " - OUT");
  }
  public synchronized static void query(String sqlQuery)
  {
    if (!query) { return; }
    logLine(timeStamp() + Symbols.sql + sqlQuery);
  }
  /**
   * Prints to screen any variable information to be viewed.
   * @param Statement The statement to print
   **/
  public synchronized static void query(String queryName, Object sqlQuery)
  {
    if (!query) { return; }
    logLine(timeStamp() + Symbols.sql + "[" + queryName + "] - " + sqlQuery);
  }
  public static void variableFormated(String string, Object... parameters)
  {
    variable(String.format(string, parameters));
  }
  public synchronized static void variable(String statement)
  {
    if (!variable) { return; }
    logLine(timeStamp() + Symbols.variable + statement);
  }
  /**
   * Prints to screen any variable information to be viewed.
   * @param Statement The statement to print
   **/
  public synchronized static void variable(String name, Object value)
  {
    if (!variable) { return; }
    logLine(timeStamp() + Symbols.variable + name + " = '" + value + "'");
  }
  private static void logLine(String text)
  {
    log(text + "\n");
  }
  private static void log(String with) throws Error
  {
    try
    {
      logTo.append(with);
    }
    catch (IOException e)
    {
      throw ObjectUtils.throwAsError(e);
    }
  }
  public synchronized static void variable(String name, Object array[])
  {
    if (!variable) { return; }
    name = (name == null ? "array" : name);
    if (array == null)
    {
      array = new Object[0];
    }
    logLine(timeStamp() + Symbols.variable + name + ".length = " + array.length);
    for (int i = 0; i < array.length; i++)
    {
      logLine(timeStamp() + name + "[" + i + "] = " + array[i]);
    }
  }
  public synchronized static <T> void variable(T array[])
  {
    variable(null, array);
  }
  public synchronized static void message(String Statement)
  {
    logLine(timeStamp() + indentMessage(Statement));
  }
  public static void event(String Statement)
  {
    if (!event) { return; }
    logLine(timeStamp() + Symbols.event + Statement);
  }
  public synchronized static void warning(String statement)
  {
    warning(statement, null);
  }
  public synchronized static void warning(Throwable throwable)
  {
    warning(null, throwable);
  }
  public synchronized static void warning(String statement, Throwable throwable)
  {
    clearHourGlass();
    logLine("******************************************************************************************");
    logLine(timeStamp());
    if (statement != null)
    {
      logLine(statement);
    }
    printFullTrace(throwable, false);
    if (throwable instanceof OutOfMemoryError)
    {
      logMemoryStatus();
    }
    logLine("******************************************************************************************");
  }
  private static void printFullTrace(Throwable throwable, boolean causedBy)
  {
    if (throwable != null)
    {
      logLine((causedBy ? "Caused by : " : "") + throwable.getClass().getName() + " -  " + throwable.getMessage());
      printStackTrace(throwable);
      if (throwable.getCause() != null)
      {
        printFullTrace(throwable.getCause(), true);
      }
    }
  }
  private static void printStackTrace(Throwable throwable)
  {
    if (!stacktraces) { return; }
    if (logTo instanceof PrintStream)
    {
      throwable.printStackTrace((PrintStream) logTo);
    }
    else if (logTo instanceof PrintStream)
    {
      throwable.printStackTrace((PrintWriter) logTo);
    }
    else
    {
      throwable.printStackTrace(new PrintWriter(new AppendableWriter(logTo)));
    }
  }
  /************************************************************************/
  /**
   * Logs the current memory status [total, used, free].
   * This forces garbage collection to run first. 
   **/
  public static void logMemoryStatus()
  {
    String memory = getMemoryStatus();
    logLine(memory);
  }
  private static String getMemoryStatus()
  {
    System.gc();
    java.text.NumberFormat format = java.text.NumberFormat.getNumberInstance();
    long freeMemory = Runtime.getRuntime().freeMemory();
    long totalMemory = Runtime.getRuntime().totalMemory();
    long usedMemory = totalMemory - freeMemory;
    String statement = "Memory [total, used, free] = [" + format.format(totalMemory) + " , "
        + format.format(usedMemory) + " , " + format.format(freeMemory) + "]";
    return statement;
  }
  /**
   * <pre>
   * {@code
   * try (Markers m = SimpleLogger.useMarkers();)
   * {
   * }
   * 
   * </pre> 
   */
  public static Markers useMarkers()
  {
    final String text = extractMarkerText();
    return new Markers(text);
  }
  /***********************************************************************/
  /***********************************************************************/
  public static StringBuffer logToString()
  {
    marker = true;
    event = true;
    variable = true;
    query = true;
    timestamp = false;
    stacktraces = false;
    StringBuffer buffer = new StringBuffer();
    logTo = buffer;
    return buffer;
  }
  public static void useOutputFile(String file, boolean addDateStamp)
  {
    if (addDateStamp)
    {
      String date = ".[" + new SimpleDateFormat("yyyy_MM_dd").format(new Date()) + "]";
      int seperator = file.lastIndexOf('.');
      if (0 < seperator)
      {
        file = file.substring(0, seperator) + date + file.substring(seperator);
      }
      else
      {
        file += System.currentTimeMillis() + ".log";
      }
    }
    try
    {
      logTo = new FileWriter(file, false);
    }
    catch (IOException e)
    {
      throw ObjectUtils.throwAsError(e);
    }
  }
  public static void logTo(Appendable writer)
  {
    logTo = writer;
  }
}