package koopa.dsl.kg.generator;

import java.nio.file.Path;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import java.util.logging.Logger;

import koopa.core.data.markers.Start;
import koopa.core.trees.Tree;
import koopa.core.trees.Trees;
import koopa.core.util.IndentingLogger;
import koopa.dsl.kg.grammar.KGGrammar;
import koopa.dsl.kg.util.KGUtil;
import koopa.templates.Template;
import koopa.templates.TemplateLogic;

public class Generation {

    private static final IndentingLogger LOGGER = new IndentingLogger(Logger.getLogger("kg.generation"));

    private final Template TEMPLATE;

    public Generation(Template template) {
        this.TEMPLATE = template;
    }

    public String generate(Path grammarFile, Tree ast) {
        // final Path path = grammarFile.getParent();

        if (LOGGER.isEnabled()) {
            LOGGER.add("Processing " + grammarFile + " ...");
        }

        // Each grammar should have an associated properties file
        // containing some extra info needed for creating a valid Java class.
        // We don't make this part of the actual grammar file
        // because we want to keep any native stuff out of there.

        var propertiesFile = KGUtil.relatedFile(grammarFile,".properties");
        var meta = KGUtil.loadProperties(propertiesFile);

        if (LOGGER.isEnabled()) {
            LOGGER.add("Loaded an additional " + meta.size() + " properties.");
        }

        // One of the things the properties file should define are all the required imports.
        // We collect them here into actual valid Java import statements.

        var additionalImports = new LinkedList<String>();
        for (var key : meta.stringPropertyNames()) {
            if (key.startsWith("import.")) {
                var importName = key.substring(7); // "import.".length()
                var packageName = meta.getProperty(key);
                additionalImports.add( "import " + packageName + "." + importName + ";" );

            } else if (key.startsWith("static.")) {
                var importName = key.substring(7); // "static.".length()
                var packageName = meta.getProperty(key);
                additionalImports.add( "import static " + packageName + "." + importName + ";" );
            }
        }

        // The order in which property names are listed is not stable.
        // So we sort all imports to ensure stability in the generated output.
        Collections.sort(additionalImports);

        if (LOGGER.isEnabled()) {
            LOGGER.add("Properties defined " + additionalImports.size() + " imports:");
            for (var ai : additionalImports) {
                LOGGER.add("* " + ai);
            }
        }

        return toCode(grammarFile, ast, meta, additionalImports);
    }

    private String toCode(Path grammarFile, Tree ast, Properties meta, List<String> additionalImports) {
        var code = new StringBuilder();
        if (!ast.isNode("grammar")) {
            throw new InternalError("Was expecting a syntax tree for a Koopa grammar." + ast);
        }
        if (LOGGER.isEnabled()) {
            LOGGER.indent("+ grammar");
        }
        TEMPLATE.apply("grammar", code, "", grammarLogic(grammarFile, ast, meta, additionalImports));
        if (LOGGER.isEnabled()) {
            LOGGER.dedent();
        }
        return code.toString();
    }

    private TemplateLogic grammarLogic(Path grammarFile, Tree ast, Properties meta,  List<String> additionalImports) {
        return new TemplateLogic() {
            /*<init>*/ {
                setValue("grammar_file", grammarFile.toString());
                setValue("name", ast.getDescendant("header", "grammar_name", "name").getAllText() );
                var base = ast.getDescendant("header", "extends", "name");
                setValue("extending", base != null ? base.getAllText() : "Koopa" );
                setValue("package", meta.getProperty("package"));
            }

            public void call(String target, StringBuilder builder, String indent) {
                if (LOGGER.isEnabled()) {
                    LOGGER.indent("+ " + target);
                }
                if ("user_imports".equals(target)) {
                    if (LOGGER.isEnabled()) {
                        LOGGER.add("Including " + additionalImports.size() + " user-defined imports.");
                    }
                    for (var imp : additionalImports) {
                        builder.append(indent);
                        builder.append(imp);
                        builder.append(System.lineSeparator());
                    }
                } else if ("rules".equals(target)) {
                    for (var rule : ast.getChildren("rule")) {
                        TEMPLATE.apply("rule", builder, indent, ruleLogic(rule));
                        var nestedRules = Trees.getMatches(rule,"nested_rule");
                        if (nestedRules != null) {
                            if (LOGGER.isEnabled()) {
                                LOGGER.indent();
                            }
                            for (var nested : nestedRules) {
                                if (nested instanceof Tree) {
                                    TEMPLATE.apply("rule", builder, indent, ruleLogic((Tree) nested));
                                }
                            }
                            if (LOGGER.isEnabled()) {
                                LOGGER.dedent();
                            }
                        }
                    }
                } else {
                    super.call(target, builder, indent);
                }
                if (LOGGER.isEnabled()) {
                    LOGGER.dedent();
                }
            }
        };
    }

    private TemplateLogic ruleLogic(Tree rule) {
        return new TemplateLogic() {
            private List<String> bindings = new LinkedList<>();
            private List<String> unbindings = new LinkedList<>();

            /*<init>*/ {
                var name = getRuleName(rule);
                var fullyQualifiedName = getFullyQualifiedRuleName(rule);
                setValue("name", name);
                setValue("fullyQualifiedName", fullyQualifiedName);
                if (LOGGER.isEnabled()) {
                    LOGGER.add("Generating rule " + fullyQualifiedName);
                }

                var allowKeywords = rule.getDescendant("nokeywords") == null;
                var hasPrivateModifier = rule.getDescendant("modifier", "private") != null;
                var hasHidingModifier = rule.getDescendant("modifier","hiding") != null;
                setValue("allowKeywords", allowKeywords ? "true" : "false");
                setValue("modifier", hasPrivateModifier ? "protected" : "public");
                setValue("visibility", hasPrivateModifier ? "PRIVATE" : hasHidingModifier ? "HIDING" : "PUBLIC");

                var locals = rule.getChild("local-variables");
                if (locals != null) {
                    var declarations = locals.getChildren("declaration");
                    for (var decl : declarations) {
                        var l = new TemplateLogic() {
                            /*<init>*/ {
                                setValue("type", decl.getChild("type").getAllText());
                                setValue("name", decl.getChild("name").getAllText());
                            }
                        };
                        bindings.add(TEMPLATE.apply("binding", l));
                        unbindings.add(TEMPLATE.apply("unbinding", l));
                    }
                }
            }

            public void call(String target, StringBuilder builder, String indent) {
                if (LOGGER.isEnabled()) {
                    LOGGER.indent("+ " + target);
                }
                if ("body".equals(target)) {
                    var sequence = rule.getChild("sequence");
                    var returnValue = rule.getChild("return_value");
                    Tree body;
                    if (returnValue == null) {
                        body = sequence;
                    } else {
                        body = new Tree(Start.on("kg", "sequence"));
                        for (var i = 0; i < sequence.getChildCount(); i++) {
                            body.addChild(sequence.getChild(i));
                        }
                        body.addChild(returnValue);
                    }
                    addPart(body, builder, indent, bindings, unbindings);
                } else {
                    super.call(target, builder, indent);
                }
                if (LOGGER.isEnabled()) {
                    LOGGER.dedent();
                }
            }
        };
    }

    private void addPart(Tree part, StringBuilder builder, String indent, List<String> bindings, List<String> unbindings) {
        var name = part.getName();
        if (("sequence".equals(name) || "nested".equals(name)) && partCount(part) == 1) {
            addFirstPart(part, builder, indent, bindings, unbindings);
        } else {
            var partial = getPartialName(name, part);
            TEMPLATE.apply(partial, builder, indent, partLogic(part, bindings, unbindings));
        }
    }

    private String unescaped(String identifier) {
        if (identifier.charAt(0) == '`') {
            return identifier.substring(1);
        } else {
            return identifier;
        }
    }

    private TemplateLogic partLogic(Tree part, List<String> bindings, List<String> unbindings) {
        return new TemplateLogic() {
            @Override
            public String getValue(String name) {
                if (name.startsWith("unescaped:")) {
                    return unescaped(getValue(name.substring("unescaped:".length())));
                }
                if (name.startsWith("unquoted:")) {
                    return unquoted(getValue(name.substring("unquoted:".length())));
                }
                if ("text".equals(name)) {
                    return part.getAllText();
                }
                if ("fully_qualified_identifier".equals(name)) {
                    return getFullyQualifiedIdentifier(part);
                }
                // Straight XPath...
                if (name.startsWith("xpath:")) {
                    return xpath(name.substring("xpath:".length()), part, null);
                }
                // XPath with a fallback value...
                if (name.startsWith("xpath[")) {
                    var index = name.indexOf("]:");
                    var defaultText = name.substring("xpath[".length(), index);
                    return xpath(name.substring(index + "]:".length()), part, defaultText);
                }
                return super.getValue(name);
            }

            public void call(String target, StringBuilder builder, String indent) {
                if (LOGGER.isEnabled()) {
                    LOGGER.indent("+ " + target);
                }
                if ("all_part".equals(target)) {
                    addAllParts(part, builder, indent, bindings, unbindings);
                } else if ("last_part".equals(target)) {
                    addLastPart(part, builder, indent, bindings, unbindings);
                } else if ("first_part".equals(target)) {
                    addFirstPart(part, builder, indent, bindings, unbindings);
                } else if ("all_dispatch_literal".equals(target)) {
                    addAllDispatchLiterals(part, builder, indent);
                } else if ("all_dispatch_sequence".equals(target)) {
                    addAllDispatchSequences(part, builder, indent, bindings, unbindings);
                } else if ("all_binding".equals(target)) {
                    if (bindings != null) {
                        for (var b : bindings) {
                            builder.append(indent);
                            builder.append(b);
                        }
                    }
                } else if ("all_unbinding".equals(target)) {
                    if (unbindings != null) {
                        for (var b : unbindings) {
                            builder.append(indent);
                            builder.append(b);
                        }
                    }
                } else {
                    super.call(target, builder, indent);
                }
                if (LOGGER.isEnabled()) {
                    LOGGER.dedent();
                }
            }
        };
    }

    private void addAllParts(Tree node, StringBuilder builder, String indent, List<String> bindings, List<String> unbindings) {
        var first = true;
        for (var i = 0; i < node.getChildCount(); i++) {
            var child = node.getChild(i);
            if (!child.isNode()) {
                continue;
            }
            var name = child.getName();
            if (!isPart(name)) {
                continue;
            }
            if (!first) {
                insertComma(builder);
            }
            addPart(child, builder, indent, bindings, unbindings);
            first = false;
        }
    }

    private void insertComma(StringBuilder builder) {
        var lastParen = builder.lastIndexOf("\n");
        builder.insert(lastParen, ',');
    }

    private void addFirstPart(Tree node, StringBuilder builder, String indent, List<String> bindings, List<String> unbindings) {
        for (var i = 0; i < node.getChildCount(); i++) {
            var child = node.getChild(i);
            if (!child.isNode()) {
                continue;
            }
            var name = child.getName();
            if (!isPart(name)) {
                continue;
            }
            addPart(child, builder, indent, bindings, unbindings);
            break;
        }
    }

    private void addLastPart(Tree node, StringBuilder builder, String indent, List<String> bindings, List<String> unbindings) {
        for (var i = node.getChildCount() - 1; i >= 0; i--) {
            var child = node.getChild(i);
            if (!child.isNode()) {
                continue;
            }
            var name = child.getName();
            if (!isPart(name)) {
                continue;
            }
            addPart(child, builder, indent, bindings, unbindings);
            break;
        }
    }

    private void addAllDispatchLiterals(Tree dispatched, StringBuilder builder, String indent) {
        var children = dispatched.getChildren("dispatch");
        for (var i = 0; i < children.size(); i++) {
            var d = children.get(i);
            builder.append(indent);
            builder.append("\"");
            appendText(builder, d.getChild("literal"));
            builder.append(i < children.size() - 1 ? "\"," : "\"");
            builder.append(System.lineSeparator());
        }
    }

    private void addAllDispatchSequences(Tree dispatched, StringBuilder builder, String indent, List<String> bindings, List<String> unbindings) {
        var children = dispatched.getChildren("dispatch");
        for (var i = 0; i < children.size(); i++) {
            var d = children.get(i);
            if (i > 0) {
                insertComma(builder);
            }
            addPart(d.getChild("sequence"), builder, indent, bindings, unbindings);
        }
    }

    private final Set<String> PART_NAMES = Set.of(
        "sequence",
        "as",
        "star",
        "plus",
        "permutation",
        "dispatched",
        "nested",
        "optional",
        "skipping",
        "negation",
        "lookahead",
        "noskip",
        "tagged",
        "ranged",
        "identifier",
        "scoped_identifier",
        "literal",
        "number",
        "quoted_literal",
        "any",
        "dot",
        "return_value",
        "before",
        "upto",
        "balancing",
        "pair",
        "balanced",
        "unbalanced",
        "closure",
        "todo",
        "notempty"
    );

    private boolean isPart(String name) {
        return PART_NAMES.contains(name);
    }

    private int partCount(Tree node) {
        var count = 0;
        for (var i = 0; i < node.getChildCount(); i++) {
            var child = node.getChild(i);
            if (!child.isNode()) {
                continue;
            }
            var name = child.getName();
            if (isPart(name)) {
                count += 1;
            }
        }
        return count;
    }

    private String getPartialName(String name, Tree node) {
        return name;
    }

    private void appendText(StringBuilder b, Tree tree) {
        // TODO Exclude comments.
        for (var t : tree.getTokens()) {
            b.append(t.getText());
        }
    }

    private String unquoted(String text) {
        if (text.startsWith("\"") || text.startsWith("'")) {
            return text.substring(1, text.length() - 1);
        } else {
            return text;
        }
    }

    private String xpath(String query, Tree part, String defaultValue) {
        var allText = Trees.getAllText(part,query);
        if (allText == null) {
            return defaultValue;
        } else {
            return allText;
        }
    }

    private String getRuleName(Tree rule) {
        return unescaped(rule.getChild("identifier").getAllText());
    }

    private String getFullyQualifiedRuleName(Tree rule) {
        var localName = getRuleName(rule);
        var parent = rule.getParent();
        if (parent == null) {
            return localName;
        } else if (parent.isNode("rule") || parent.isNode("nested_rule")) {
            return getFullyQualifiedRuleName(parent) + KGGrammar.SCOPE_SEPARATOR + localName;
        } else {
            return localName;
        }
    }

    private String getFullyQualifiedIdentifier(Tree identifier) {
        // This name may be escaped. Which is fine, we'll match it against another escaped name anyway.
        var name = identifier.getAllText();
        // Find the rule this identifier is being used in.
        var rule = getOwningScope(identifier);
        while (rule != null) {
            // Check all rules which are defined directly inside it.
            var nestedRules = rule.getChildren("nested_rule");
            for (var nestedRule : nestedRules) {
                var nestedRuleName = nestedRule.getChild("identifier").getAllText();
                // If the names match, return the fully qualified name for the rule we found.
                if (name.equals(nestedRuleName)) {
                    return getFullyQualifiedRuleName(nestedRule);
                }
            }
            // If there is no match in the scope of this rule then move up to the scope which defines this rule.
            rule = getOwningScope(rule);
        }
        // If we find no match at all we just assume the name is a global and return it (unescaped).
        return unescaped(name);
    }

    /**
     * Retrieves the scope (rule or nested_rule) the given item was defined in.
     */
    private Tree getOwningScope(Tree item) {
        var parent = item.getParent();
        while (parent != null && !parent.isNode("rule") && !parent.isNode("nested_rule")) {
            parent = parent.getParent();
        }
        return parent;
    }

}
