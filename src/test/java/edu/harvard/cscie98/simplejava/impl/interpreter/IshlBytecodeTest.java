package edu.harvard.cscie98.simplejava.impl.interpreter;

import static org.junit.Assert.assertEquals;

import org.apache.bcel.Constants;
import org.junit.Before;
import org.junit.Test;

public class IshlBytecodeTest extends BytecodeTest {

  @Override
  @Before
  public void setUp() {
    super.setUp();
    frame.push(1);
    frame.push(2);
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
    return new byte[] { (byte) Constants.ISHL };
  }

  @Test
  public void testStackLayout() {
    interpreter.interpretBytecode(frame);
    assertEquals(1, frame.getStack().size());
    assertEquals(4, frame.pop());
  }

  @Test
  public void testProgramCounter() {
    interpreter.interpretBytecode(frame);
    assertEquals(1, frame.getProgramCounter());
  }

}
