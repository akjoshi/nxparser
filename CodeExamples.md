# Code Examples #

  * Read Nx from a file:
```
FileInputStream is = new FileInputStream("path/to/file.nq");

NxParser nxp = new NxParser(is);

Node[] nxx;
while (nxp.hasNext())
     nxx = test.next();
```
> > We'd like to point you [here](http://captsolo.net/info/blog_a.php/2011/02/21/p1094) for hints on how to use the classes from Python (thanks to Uldis Bojars).
  * Use a blank node:
```
Resource subjRes = new Resource("<http://example.org/123>", true);
Resource predRes = new Resource("<http://example.org/123>", true);
BNode bn = new BNode("_:bnodeId", true);

Node[] triple = new Node[]{subjRes, predRes, bn};
		
System.out.printf("%s %s %s .", triple[0].toN3(), triple[1].toN3(), triple[2].toN3());
// yields <http://example.org/123> <http://example.org/123> _:bnodeId .
```
  * Use a date literal:
```
String dateString = "2011-08-16";
Literal dateLiteral = new Literal(dateString, new Resource(XMLConstants.W3C_XML_SCHEMA_NS_URI + "#date"));

// or build the Nx compatible Literal string yourself:
String dateInN3 = String.format("\"%s\"^^<%s%s>", dateString, XMLConstants.W3C_XML_SCHEMA_NS_URI, "#date");
dateLiteral = new Literal(dateInN3, true);

Node[] triple = new Node[]{bn, predRes, dateLiteral};

System.out.printf("%s %s %s .", triple[0].toN3(), triple[1].toN3(), triple[2].toN3());
// yields _:bnodeId <http://example.org/123> "2011-08-16"^^<http://www.w3.org/2001/XMLSchema#date> .
```
  * Use unicode characters
```
String japaneseString = ("祝福は、チーズのメーカーです。");
Literal japaneseLiteral = new Literal(NxUtil.escapeForNx(japaneseString), "ja");
		
Node[] triple = new Node[]{subjRes, predRes, japaneseLiteral};
		
System.out.printf("%s %s %s .", triple[0].toN3(), triple[1].toN3(), triple[2].toN3());
// yields <http://example.org/123> <http://example.org/123> "\u795D\u798F\u306F\u3001\u30C1\u30FC\u30BA\u306E\u30E1\u30FC\u30AB\u30FC\u3067\u3059\u3002"@ja .

// To get the utf characters back use:
System.out.println(japaneseLiteral.getUnescapedData());
// yields 祝福は、チーズのメーカーです。
```

  * Get a Calendar object from an xsd:dateTime-typed Literal
```
Literal dtl; // parser-generated
XSDDateTime dt = (XSDDateTime)DatatypeFactory.getDatatype(dtl); 
GregorianCalendar cal = dt.getValue();
```