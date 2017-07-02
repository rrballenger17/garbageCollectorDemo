package edu.harvard.cscie98.simplejava.impl.interpreter;

import static org.junit.Assert.assertEquals;

import org.apache.bcel.Constants;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.cscie98.simplejava.vm.classloader.TypeFactory;
import edu.harvard.cscie98.simplejava.vm.objectmodel.HeapPointer;

public class AstoreBytecodeTest extends BytecodeTest {

  private HeapPointer ref;

  @Override
  @Before
  public void setUp() {
    super.setUp();
    ref = getRef(getObjectDescriptor(TypeFactory.fromBinaryName("java.lang.Object")));
    frame.push(ref);
  }

  @Override
  protected int getMaxStack() {
    return 1;
  }

  @Override
  protected int getMaxLocals() {
    return 9;
  }

  @Override
  protected byte[] getBytes() {
    return new byte[] { (byte) Constants.ASTORE, 8 };
  }

  @Test
  public void testStackLayout() {
    interpreter.interpretBytecode(frame);
    assertEquals(0, frame.getStack().size());
  }

  @Test
  public void testLocals() {
    interpreter.interpretBytecode(frame);
    assertEquals(ref, frame.getLocalVariable(8));
  }

  @Test
  public void testProgramCounter() {
    interpreter.interpretBytecode(frame);
    assertEquals(2, frame.getProgramCounter());
  }

}
