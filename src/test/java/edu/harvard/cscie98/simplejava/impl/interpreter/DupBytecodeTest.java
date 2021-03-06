package edu.harvard.cscie98.simplejava.impl.interpreter;

import static org.junit.Assert.assertEquals;

import org.apache.bcel.Constants;
import org.junit.Before;
import org.junit.Test;

public class DupBytecodeTest extends BytecodeTest {

  private Object v1;

  @Override
  @Before
  public void setUp() {
    super.setUp();
    v1 = new Object();
    frame.push(v1);
  }

  @Override
  protected int getMaxStack() {
    return 2;
  }

  @Override
  protected int getMaxLocals() {
    return 0;
  }

  @Override
  protected byte[] getBytes() {
    return new byte[] { (byte) Constants.DUP };
  }

  @Test
  public void testStackLayout() {
    interpreter.interpretBytecode(frame);
    assertEquals(2, frame.getStack().size());
    assertEquals(v1, frame.pop());
    assertEquals(v1, frame.pop());
  }

  @Test
  public void testProgramCounter() {
    interpreter.interpretBytecode(frame);
    assertEquals(1, frame.getProgramCounter());
  }

}
