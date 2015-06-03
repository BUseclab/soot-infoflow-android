package soot.jimple.infoflow.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.ThisRef;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.util.Chain;

/**
 * Locate the Fragments used the APK 
 * 
 * @author Wil Koch
 * @see {LibraryClassPatcher} class for examples of creating and applying changes to scene
 */
public class RewireFlow implements PreAnalysisHandler {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	
	private List<String> asyncTaskClasses = new ArrayList<String>();
	private List<SootMethod> onPostExecuteMethods = null;

	public void rewire(){
		Chain<SootClass> classes = Scene.v().getClasses();
		Iterator<SootClass> it = classes.iterator();
		while (it.hasNext()){
			SootClass sootClass = it.next();			
			if (!sootClass.getName().startsWith("android") && hasSuperClass(sootClass, Collections.singletonList("android.os.AsyncTask"))){
				//transformAsyncTask(sootClass);
				asyncTaskClasses.add(sootClass.getName());
			}
		}
		asyncTaskRewireTransformer();
	}

	/**
	 * Go up the hierarchy and see if it has a parent 
	 * @param sootClass
	 * @param superClass
	 * @return
	 */
	private boolean hasSuperClass(SootClass sootClass, List<String> superClasses){
		String className = sootClass.getName();
		if (className.equals("java.lang.Object")){
			return false;
		} else if (superClasses.contains(className)){
			return true;
		} else {
			if (!sootClass.hasSuperclass()){
				return false;
			} else {
				SootClass superClass = sootClass.getSuperclass();
				return hasSuperClass(superClass, superClasses);
			}
		}
	}
	
	private SootMethod getCorrespondingonPostExecuteMethod(SootMethod doInBackgroundMethod){
		Type returnType = doInBackgroundMethod.getReturnType();
		for (SootMethod onPostExecuteMethod : onPostExecuteMethods){
			
			//By the signature, it has only 1 param
			Type paramType = onPostExecuteMethod.getParameterTypes().get(0);
			if (paramType.equals(returnType)){
				return onPostExecuteMethod;
			}
		}
		return null;
	}
	private Local getLocal(SootClass sc, Body b){
		/*
		LocalGenerator generator = new LocalGenerator(body);
		ThisRef thisRef = Jimple.v().newThisRef(c.getType());
		
		Local l = generator.generateLocal(thisRef.getType());
		*/
		Local thisLocal = Jimple.v().newLocal("$r0", sc.getType());
		
		/*
		Local thisLocal = Jimple.v().newLocal("this", sc.getType());
		b.getLocals().add(thisLocal);
		b.getUnits().addFirst(Jimple.v().newIdentityStmt(thisLocal,
				Jimple.v().newThisRef(sc.getType())));
		*/
		return thisLocal;
	}
	private void rewireDoInBackground(SootClass currentClass, final SootMethod doInBackgroundMethod){
		
		final Body body = doInBackgroundMethod.retrieveActiveBody();
		final Local local = body.getThisLocal();
		final SootMethod onPostExecuteMethod = getCorrespondingonPostExecuteMethod(doInBackgroundMethod);
		final PatchingChain<Unit> units = body.getUnits();
		
		for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
			final Unit u = iter.next();
			u.apply(new AbstractStmtSwitch() {
				
				//TODO fix case for more complex return types
			    public void caseReturnStmt(ReturnStmt stmt)
			    {
			    	Value value = stmt.getOpBox().getValue();
			    	if (! (value instanceof NullConstant)){
			    		InvokeExpr vie = Jimple.v().newVirtualInvokeExpr(local, onPostExecuteMethod.makeRef(), value);
			    		InvokeStmt onPostExecuteStatement = Jimple.v().newInvokeStmt(vie);
			    	
			    		units.insertBefore(onPostExecuteStatement, stmt);
			    	}
			    }

			    public void caseReturnVoidStmt(ReturnVoidStmt stmt)
			    {
			    	logger.debug("statment");
			    }
				
			});
		}
	}
	private void rewireAsyncTaskClass(SootClass currentClass){
		onPostExecuteMethods = new ArrayList<SootMethod>();
		List<SootMethod> methods = currentClass.getMethods();
		//First get all postexecute methods. One will be super, other implemented
		for (SootMethod method : methods){
			if (method.getName().startsWith("onPostExecute")){
		    	onPostExecuteMethods.add(method);
			}
		}
		if (!onPostExecuteMethods.isEmpty()){
			for (SootMethod method : methods){
				if (!method.isAbstract() && method.getName().startsWith("doInBackground")){
					rewireDoInBackground(currentClass, method);
				}
			}
		}
	}
	
	
	private void asyncTaskRewireTransformer2(){
		Transform transform = new Transform("wjtp.asyc", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				
				/*
				for (SootClass sootClass : Scene.v().getApplicationClasses()){
					if (!sootClass.getName().startsWith("android") && hasSuperClass(sootClass, Collections.singletonList("android.os.AsyncTask"))){
						logger.debug("Async class {}", sootClass.getName());
						rewireAsyncTaskClass(sootClass);

					}
				}
				*/
				
				for (String className : asyncTaskClasses) {
					SootClass sootClass = Scene.v().getSootClass(className);
					sootClass.setApplicationClass();
					logger.debug("Async class {}", sootClass.getName());
					rewireAsyncTaskClass(sootClass);
				}
				System.out.println("Callback analysis done.");

				
			}
		});

		PackManager.v().getPack("wjtp").add(transform);
		PackManager.v().getPack("wjtp").apply();
    	logger.debug("Done rewire");

    	printAsyncBodies();
		
	}
	
	private void asyncTaskRewireTransformer(){
		for (String className : asyncTaskClasses) {
			SootClass sootClass = Scene.v().getSootClass(className);
			//SootClass sootClass = Scene.v().forceResolve(className, SootClass.BODIES);
			//sootClass.setApplicationClass();
			logger.debug("Async class {}", sootClass.getName());
			rewireAsyncTaskClass(sootClass);
		}
		/*
		Scene.v().releaseActiveHierarchy();
		Scene.v().releaseFastHierarchy();
		Scene.v().getOrMakeFastHierarchy();
		Scene.v().releaseReachableMethods();
		Scene.v().releaseCallGraph();
		
		Scene.v().loadNecessaryClasses();
		
		//Scene.v().getCallGraph().addEdge(null);
		printAsyncBodies();
		*/
	}
	
	
	public void printAsyncBodies(){
		for (String className : asyncTaskClasses) {
			SootClass sootClass = Scene.v().getSootClass(className);
			logger.debug("*****************************Async {}", sootClass);

			List<SootMethod> methods = sootClass.getMethods();

			for (SootMethod method : methods){
				if (method.getName().startsWith("doInBackground")){
					Body body = method.retrieveActiveBody();
					logger.trace("{}", body);

				}
			}
		}
	}
	private boolean isSuperClass(SootClass currentClass, List<String> superClasses){
		boolean isSuper = false;
		List<SootClass> extendedClasses = Scene.v().getActiveHierarchy().getSuperclassesOf(currentClass);
		for(SootClass sc : extendedClasses) {
			String superName = sc.getName();
			if (superClasses.contains(superName)){
				isSuper = true;
				break;
			}
		}
		return isSuper;
	}

	@Override
	public void onBeforeCallgraphConstruction() {
		rewire();
	}

	@Override
	public void onAfterCallgraphConstruction() {
    	//logger.debug("{}",Scene.v().getCallGraph());
		
	}

}
