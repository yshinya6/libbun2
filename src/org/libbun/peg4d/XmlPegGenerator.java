package org.libbun.peg4d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.libbun.Main;
import org.libbun.peg4d.PegObject;
import org.libbun.UMap;

public class XmlPegGenerator {

	int arrayCount = 0;
	UMap<Integer> NameMap = new UMap<Integer>();

	public final StringSource generate(StringSource source, PegObject node, int index) {
		int count = 0;
		for(int i = 0; i < node.AST.length; i++) {
			switch (node.AST[i].tag) {
			case "#element":
				index = this.NameMap.get(node.AST[i].AST[0].getText());
				source.sourceText += "Element" + index + " = << _* '<' ElementName" + index + "@'>' _* " + "Members" + index + " _* '</' ElementName" + index + "'>' _* #element >>;\n\n";
				source = generate(source,node.AST[i], index);
				break;

			case "#elementName":
				source.sourceText += "ElementName" + index + " = <<\"" + node.AST[i].getText() + "\" #string>>;\n\n";
				break;

			case "#member":
				source = generate(source, node.AST[i], index);
				source.sourceText += "Members" + index + " =";
				for(int j = 0; j < node.AST[i].AST.length - 1; j++) {
					source.sourceText += " Member" + index + "_" + j + "@";
				}
				source.sourceText += " Member" + index + "_" + (node.AST[i].AST.length - 1) + "@;\n\n";
				break;

			case "#docTypeName": //top of DTD
				source.sourceText += "Element0 = '<'\""+ node.AST[i].getText() + "\"'>' _*  Member0@ _* '</'\"" + node.AST[i].getText() + "\"'>';\n\n";
				source.sourceText += "Member0 = << Element1@ #member>>;\n\n";
				break;

			case "#memberName":
				source.sourceText += "Member" + index + "_" + count + " = << Element" + this.NameMap.get(node.AST[i].getText()) +"@ #member>>;\n\n";
				count++;
				break;

			case "#data":
				source.sourceText += "Member" + index + "_" + count + " = << CharData #data >>;\n\n";
				count++;
				break;
			}
		}
		return source;
	}

	public final String generateXmlPegFile(PegObject node) {
		StringSource source = (StringSource) Main.loadSource("sample/rootXml.peg");
		this.getElementName(node);
		this.generate(source, node, 0);
		String LanguageXmlPeg = "sample/generatedXml.peg";
		File newfile = new File(LanguageXmlPeg);
		try{
			newfile.createNewFile();
			File file = new File(LanguageXmlPeg);
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(source.sourceText);
			fileWriter.close();
		}catch(IOException e){
			System.out.println(e);
		}
		return LanguageXmlPeg;
	}

	public final void getElementName(PegObject node) {
		for(int i = 0; i < node.AST.length; i++) {
			if(node.AST[i].tag.equals("#docTypeName")) {
				this.NameMap.put(node.AST[i].getText(), 0);
			}
			else {
				this.NameMap.put(node.AST[i].AST[0].getText(), i);
			}
		}
	}
}
