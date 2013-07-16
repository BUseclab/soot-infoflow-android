package soot.jimple.infoflow.android.test.droidBench;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import soot.jimple.infoflow.InfoflowResults;

public class CallbackTests extends JUnitTests {
	
	@Test
	public void runTestAnonymousClass1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_AnonymousClass1.apk");
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestButton1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Button1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestButton2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Button2.apk");
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestTestButton3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Button3.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestButton4() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Button4.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestLocationLeak1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_LocationLeak1.apk");
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestLocationLeak2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_LocationLeak2.apk");
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestLocationLeak3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_LocationLeak3.apk");
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestMethodOverride1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_MethodOverride1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestMultiHandlers1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_MultiHandlers1.apk");
		Assert.assertEquals(0, res.size());
	}

	@Test
	public void runTestOrdering1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Ordering1.apk");
		Assert.assertEquals(0, res.size());
	}

	@Test
	public void runTestRegisterGlobal1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_RegisterGlobal1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestRegisterGlobal2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_RegisterGlobal2.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestUnregister1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Unregister1.apk");
		Assert.assertEquals(0, res.size());
	}

}