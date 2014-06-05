package org.libbun;

import org.libbun.SymbolTable.DefinedNameFunctor;

public class KonohaTypeChecker {
	
	public static void typeLet(PegObject node) {
//		System.out.println("TypeDeclCommand node: " + node);
		SymbolTable gamma = node.getSymbolTable();
		MetaType type = node.typeAt(gamma, 1, MetaType.UntypedType);
		gamma.checkTypeAt(node, 2, type, null, true);
		if(node.findParentNode("#function") == null) {
			gamma.setGlobalName(node.get(0), type, node.get(2));
		}
		else {
			PegObject block = node.findParentNode("#block");
			block.setName(node.get(0), type, node.get(2));
		}
	}

	public static void typeFunction(PegObject node) {
		//System.out.println("FuncDeclCommand node: " + node);
		SymbolTable gamma = node.getSymbolTable();
		if(node.typed != null) {
			VarType	inf = new VarType(node.get(0), FuncType.FuncBaseName);
			PegObject params = node.get(1, null);
			PegObject block = node.get(3, null);
			for(int i = 0; i < params.size(); i++) {
				PegObject p = params.get(i);
				p.typeAt(gamma, 1, null);
				MetaType ptype = inf.newVarType(p.get(1));
				if(block != null) {
					block.setName(p.get(0), ptype, null);
				}
			}
			node.typeAt(gamma, 2, null);
			MetaType returnType = inf.newVarType(node.get(2));
			node.typed = inf.getRealType();
			if(block != null) {
				block.setName(node.get(0), node.typed, node);
				block.setName("return", returnType, null);
				System.out.println("@@@@@@@ " + gamma.tryMatch(block));
			}
			gamma.setFunctionName(node.getTextAt(0, "f"), node.typed, node);
		}
	}

	public static void typeCheckApply(PegObject node) {
		System.out.println("typeCheckApply: " + node);
		SymbolTable gamma = node.getSymbolTable();

	}

	public static void typeCheckReturn(PegObject node) {
		//System.out.println("FuncDeclCommand node: " + node);
		SymbolTable gamma = node.getSymbolTable();
		DefinedNameFunctor f = gamma.getName("return");
		MetaType returnType = f.getReturnType(MetaType.UntypedType);
		System.out.println("returnType="+returnType);
		gamma.checkTypeAt(node, 0, returnType, null, false);
	}

	
	public static void initDriver(BunDriver driver) {
		class TypeCommand extends DriverCommand {
			@Override
			public void invoke(BunDriver driver, PegObject node, String[] param) {
				PegObject parent = node.parent;
				if(parent.is("#let") || parent.is("#var")) {
					KonohaTypeChecker.typeLet(parent);
					return;
				}
				if(parent.is("#function")) {
					KonohaTypeChecker.typeFunction(parent);
					return;
				}
			}
		}
		driver.addCommand("type", new TypeCommand());
		class TypeCheckCommand extends DriverCommand {
			@Override
			public void invoke(BunDriver driver, PegObject node, String[] param) {
				if(node.is("#apply")) {
					KonohaTypeChecker.typeCheckApply(node);
					return;
				}
				if(node.is("#return")) {
					KonohaTypeChecker.typeCheckReturn(node);
					return;
				}
			}
		}
		driver.addCommand("typecheck", new TypeCheckCommand());
	}
	
	
}
