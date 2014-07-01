package org.libbun;

public class BunTypeChecker {
	// SymbolTable
	private class DefinedNameFunctor extends Functor {
		PegObject defNode = null;
		public DefinedNameFunctor(int flag, String name, BunType type, PegObject defNode) {
			super(flag, name, type);
			this.defNode = defNode;
		}
		@Override
		public void build(PegObject node, BunDriver driver) {
			driver.pushLocalName(node, this.name);
		}
	}
	private DefinedNameFunctor getName(SymbolTable gamma, String name) {
		Functor f = gamma.getSymbol(name);
		if(f instanceof DefinedNameFunctor) {
			return (DefinedNameFunctor)f;
		}
		return null;
	}
	private Functor setName(SymbolTable gamma, int flag, PegObject nameNode, BunType type, PegObject defNode) {
		String name = nameNode.getText();
		Functor defined = gamma.getLocalSymbol(name);
		if(defined != null) {
			gamma.report(nameNode, "notice", "duplicated name: " + name);
		}
		type = type.getRealType();
		if(type.hasVarType()) {
			gamma.report(nameNode, "notice", "ambigious variable: " + name + " :" + type.getName());
		}
		DefinedNameFunctor f = new DefinedNameFunctor(flag, name, type, defNode);
		gamma.setSymbol(name, f);
		return f;
	}


//
//class DefinedGlobalNameFunctor extends DefinedNameFunctor {
//	public DefinedGlobalNameFunctor(String name, BunType type, PegObject initValue) {
//		super(name, type, initValue);
//	}
//	@Override
//	public void build(PegObject node, BunDriver driver) {
//		driver.pushGlobalName(node, this.name);
//	}
//}
//
//class DefinedFunctionFunctor extends DefinedNameFunctor {
//	public DefinedFunctionFunctor(String name, BunType type, PegObject funcNode, Functor defined) {
//		super(name, type, funcNode);
//		this.nextChoice = defined;
//	}
//	@Override
//	public void build(PegObject node, BunDriver driver) {
//		driver.pushApplyNode(node, name);
//	}
//}
//
//
//public Functor setName(String name, BunType type, PegObject initValue) {
//	return this.setName(false, name, null, type, initValue);
//}
//
//public Functor setName(PegObject nameNode, BunType type, PegObject initValue) {
//	return this.setName(false, nameNode.getText(), nameNode, type, initValue);
//}
//
//public Functor setGlobalName(PegObject nameNode, BunType type, PegObject initValue) {
//	return this.setName(true, nameNode.getText(), nameNode, type, initValue);
//}
//
//public Functor setFunctionName(String name, BunType type, PegObject node) {
//	Functor defined = this.getSymbol(name);
//	defined = new DefinedFunctionFunctor(name, type, node, defined);
//	if(Main.EnableVerbose) {
//		Main._PrintLine(name + " :" + type.getName() + " initValue=" + node);
//	}
//	this.setSymbol(name, defined);
//	return defined;
//}
//
//

	abstract class TypeChecker {
		public abstract PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping);
	}
	class VarChecker extends TypeChecker {
		@Override
		public PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
			int flag = Functor._SymbolFunctor;
			BunType type = node.typeAt(gamma, 1, BunType.UntypedType);
			gamma.checkTypeAt(node, 2, type, true);
			if(node.findParentNode("#function") == null) {
			}
			else {
				PegObject block = node.findParentNode("#block");
				gamma = block.getLocalSymbolTable();
				flag |= Functor._ReadOnlyFunctor;
			}
			setName(gamma, flag, node.get(0), type, node);
			return node;
		}
	}
	class NameChecker extends TypeChecker {
		@Override
		public PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
			String name = node.getText();
			DefinedNameFunctor f = getName(gamma, name);
			if(f != null) {
				node.matched = f;
				node.typed = f.getReturnType(BunType.UntypedType);
				return node;
			}
			return gamma.typeErrorNode(node, "undefined name: " + name, isStrongTyping);
		}
	}
//
//	class FunctionChecker extends TypeChecker {
//		@Override
//		public void check(PegObject node) {
//			//System.out.println("FuncDeclCommand node: " + node);
//			SymbolTable gamma = node.getSymbolTable();
//			if(node.typed != null) {
//				PegObject params = node.get(1, null);
//				PegObject block = node.get(3, null);
//				UniArray<BunType> typeList = new UniArray<BunType>(new BunType[params.size()+1]);
//				for(int i = 0; i < params.size(); i++) {
//					PegObject p = params.get(i);
//					BunType ptype = p.typeAt(gamma, 1, null);
//					typeList.add(ptype);
//					if(block != null) {
//						block.setName(p.get(0), ptype, null);
//					}
//				}
//				BunType returnType = node.typeAt(gamma, 2, null);
//				typeList.add(returnType);
//				node.typed = BunType.newFuncType(typeList);
//				if(block != null) {
//					block.setName(node.get(0), node.typed, node);
//					block.setName("return", returnType, null);
//					//block.gamma.tryMatch(block);
//				}
//				gamma.setFunctionName(node.textAt(0, "f"), node.typed, node);
//			}
//		}
//	}
//	class ApplyChecker extends TypeChecker {
//		@Override
//		public void check(PegObject node) {
//			System.out.println("typeCheckApply: " + node);
////			SymbolTable gamma = node.getSymbolTable();
//		}
//	}
//	class FuncDecl extends TypeChecker {
//		@Override
//		public void check(PegObject node) {
//			//System.out.println("FuncDeclCommand node: " + node);
//			SymbolTable gamma = node.getSymbolTable();
//			for(int i = 0; i < node.size(); i++) {
//				gamma.checkTypeAt(node, i, BunType.VoidType, false);
//			}
//			DefinedNameFunctor f = gamma.getName("return");
//			BunType returnType = f.getReturnType(BunType.UntypedType);
//			System.out.println("returnType="+returnType);
//			gamma.checkTypeAt(node, 0, returnType, false);
//		}
//	}
//	class ApplyChecker extends TypeChecker {
//		@Override
//		public void check(PegObject node) {
//			public static void typeCheckReturn(PegObject node) {
//				//System.out.println("FuncDeclCommand node: " + node);
//				SymbolTable gamma = node.getSymbolTable();
//				DefinedNameFunctor f = gamma.getName("return");
//				BunType returnType = f.getReturnType(BunType.UntypedType);
//				System.out.println("returnType="+returnType);
//				gamma.checkTypeAt(node, 0, returnType, false);
//			}
//		}
//	}
	
	private static UniMap<TypeChecker> checkerMap = null;

	public BunTypeChecker() {
		if(checkerMap == null) {
			checkerMap = new UniMap<TypeChecker>();
			checkerMap.put("#var:3", new VarChecker());
			checkerMap.put("#name",  new NameChecker());
		}
	}

	public final static PegObject check(SymbolTable gamma, PegObject node, boolean isStrongTyping) {
		String nodeKey = node.tag;
		if(node.size() < 4) {
			nodeKey += ":" + node.size();
		}
		TypeChecker c = checkerMap.get(nodeKey, null);
		if(c != null) {
			return c.check(gamma, node, isStrongTyping);
		}
		return node;
	}
	
}
