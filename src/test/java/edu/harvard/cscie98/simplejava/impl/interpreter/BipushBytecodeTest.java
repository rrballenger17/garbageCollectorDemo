package edu.harvard.cscie98.simplejava.impl.interpreter;

import static org.junit.Assert.assertEquals;

import org.apache.bcel.Constants;
import org.junit.Before;
import org.junit.Test;

public class BipushBytecodeTest extends BytecodeTest {

  @Override
  @Before
  public void setUp() {
    super.setUp();
  }

  @Override
  protected int getMaxStack() {
    return 1;
  }

  @Override
  protected int getMaxLocals() {
    return 0;
  }

  @Override
  protected byte[] getBytes() {
    return new byte[] { (byte) Constants.BIPUSH, 42 };
  }

  @Test
  public void testStackLayout() {
    interpreter.interpretBytecode(frame);
    assertEquals(1, frame.getStack().size());
    assertEquals(42, frame.pop());
  }

  @Test
  public void testProgramCounter() {
    interpreter.interpretBytecode(frame);
    assertEquals(2, frame.getProgramCounter());
  }

}
