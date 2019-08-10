package koopa.core.grammars.fluent;

import static koopa.core.grammars.combinators.Scoped.Visibility.PUBLIC;

import java.util.HashMap;
import java.util.Map;

import koopa.core.grammars.Grammar;
import koopa.core.grammars.combinators.MatchAny;
import koopa.core.grammars.combinators.MatchEndOfFile;
import koopa.core.grammars.combinators.MatchKeyword;
import koopa.core.grammars.combinators.MatchToken;
import koopa.core.grammars.combinators.OptimizingChoice;
import koopa.core.grammars.combinators.Scoped;
import koopa.core.grammars.combinators.TestForCase;
import koopa.core.grammars.combinators.TestForKeyword;
import koopa.core.grammars.combinators.TestTag;
import koopa.core.parsers.FutureParser;
import koopa.core.parsers.Optimizer;
import koopa.core.parsers.ParserCombinator;
import koopa.core.parsers.combinators.At;
import koopa.core.parsers.combinators.Choice;
import koopa.core.parsers.combinators.Not;
import koopa.core.parsers.combinators.Opt;
import koopa.core.parsers.combinators.Optional;
import koopa.core.parsers.combinators.Plus;
import koopa.core.parsers.combinators.Sequence;
import koopa.core.parsers.combinators.SkipTo;
import koopa.core.parsers.combinators.Star;
import koopa.core.parsers.combinators.WithOption;

public abstract class FluentGrammar extends Grammar {

    private final Map<String, DelayedDefinition> definitions = new HashMap<>();

    protected NamedDefinition define(String name) {
        assert (!definitions.containsKey(name));
        var def = new NamedDefinition(name);
        definitions.put(name, def);
        return def;
    }

    protected DelayedDefinition defineHelper(String name) {
        assert (!definitions.containsKey(name));
        var def = new DelayedDefinition();
        definitions.put(name, def);
        return def;
    }

    protected NamedDefinition with(String name) {
        return new NamedDefinition(name);
    }

    protected Definition definitionOf(String name) {
        return definitions.get(name);
    }

    protected Definition all(Object... elements) {
        return new Definition() {
            @Override
            public ParserCombinator asParser() {
                if (elements.length == 1) {
                    return convertToParser(elements[0]);
                } else {
                    return new Sequence(convertAllToParsers(elements));
                }
            }
        };
    }

    protected Definition oneOf(Object... elements) {
        return new Definition() {
            @Override
            public ParserCombinator asParser() {
                if (elements.length == 1) {
                    return convertToParser(elements[0]);
                } else if (Optimizer.shouldRun()) {
                    return new OptimizingChoice(FluentGrammar.this, convertAllToParsers(elements));
                } else {
                    return new Choice(convertAllToParsers(elements));
                }
            }
        };
    }

    protected Definition many(Object... elements) {
        return new Definition() {
            final Definition def = all(elements);
            @Override
            public ParserCombinator asParser() {
                return new Star(def.asParser());
            }
        };
    }

    protected Definition oneOrMore(Object... elements) {
        return new Definition() {
            final Definition def = all(elements);
            @Override
            public ParserCombinator asParser() {
                return new Plus(def.asParser());
            }
        };
    }

    protected Definition skipTo(Object... elements) {
        return new Definition() {
            final Definition def = all(elements);
            @Override
            public ParserCombinator asParser() {
                return new SkipTo(def.asParser());
            }
        };
    }

    protected Definition optional(Object... elements) {
        return new Definition() {
            final Definition def = all(elements);
            @Override
            public ParserCombinator asParser() {
                return new Optional(def.asParser());
            }
        };
    }

    protected Definition alternating(Object element, Object separator) {
        return all(element, many(separator, element));
    }

    protected Definition noskip(Object... elements) {
        return new Definition() {
            final Definition def = all(elements);
            @Override
            public ParserCombinator asParser() {
                return new WithOption(Opt.NOSKIP, def.asParser());
            }
        };
    }

    protected Definition token(String text) {
        return new Definition() {
            @Override
            public ParserCombinator asParser() {
                return new MatchToken(FluentGrammar.this, text);
            }
        };
    }

    protected Definition keyword(String text) {
        return new Definition() {
            @Override
            public ParserCombinator asParser() {
                return new MatchKeyword(FluentGrammar.this, text);
            }
        };
    }

    protected Definition not(Object... elements) {
        return new Definition() {
            final Definition def = all(elements);
            @Override
            public ParserCombinator asParser() {
                return new Not(def.asParser());
            }
        };
    }

    protected Definition tagged(Object tag) {
        return new Definition() {
            @Override
            public ParserCombinator asParser() {
                return new TestTag(FluentGrammar.this, tag);
            }
        };
    }

    protected Definition any() {
        return new Definition() {
            @Override
            public ParserCombinator asParser() {
                return new MatchAny(FluentGrammar.this);
            }
        };
    }

    protected Definition eof() {
        return new Definition() {
            @Override
            public ParserCombinator asParser() {
                return new MatchEndOfFile(FluentGrammar.this);
            }
        };
    }

    protected Definition allUppercase(Object... elements) {
        return new Definition() {
            @Override
            public ParserCombinator asParser() {
                return new TestForCase(FluentGrammar.this, true, true, all(elements).asParser());
            }
        };
    }

    protected Definition someLowercase(Object... elements) {
        return new Definition() {
            @Override
            public ParserCombinator asParser() {
                return new TestForCase(FluentGrammar.this, true, false, all(elements).asParser());
            }
        };
    }

    protected Definition notAKeyword(Object... elements) {
        return new Definition() {
            @Override
            public ParserCombinator asParser() {
                return new TestForKeyword(FluentGrammar.this, false, all(elements).asParser());
            }
        };
    }

    protected Definition at(Object... elements) {
        return new Definition() {
            @Override
            public ParserCombinator asParser() {
                return new At(all(elements).asParser());
            }
        };
    }

    @Override
    public ParserCombinator keyword() {
        return any().asParser();
    }

    protected ParserCombinator convertToParser(Object object) {
        if (object instanceof ParserCombinator) {
            return (ParserCombinator) object;
        } else if (object instanceof Definition) {
            return ((Definition) object).asParser();
        } else if (object instanceof String) {
            var text = (String) object;
            if (text.startsWith("==") && text.endsWith("==")) {
                return token(text.substring(2, text.length() - 2)).asParser();
            } else if (text.startsWith("++") && text.endsWith("++")) {
                return keyword(text.substring(2, text.length() - 2)).asParser();
            } else if (definitions.containsKey(text)) {
                return definitionOf(text).asParser();
            } else {
                throw new InternalError("Don't know how to convert: " + text);
            }
        } else {
            throw new InternalError("Don't know how to convert: " + object);
        }
    }

    protected ParserCombinator[] convertAllToParsers(Object... elements) {
        var pcs = new ParserCombinator[elements.length];
        for (var i = 0; i < elements.length; i++) {
            pcs[i] = convertToParser(elements[i]);
        }
        return pcs;
    }

    public abstract class Definition {
        public abstract ParserCombinator asParser();
    }

    public class DelayedDefinition extends Definition {
        Definition body = null;
        FutureParser parser = null;

        public DelayedDefinition as(Object... elements) {
            assert (body == null);
            body = all(elements);
            return this;
        }

        public DelayedDefinition with(String name) {
            assert (body == null);
            var named = new NamedDefinition(name);
            body = named;
            return named;
        }

        @Override
        public ParserCombinator asParser() {
            if (parser == null) {
                parser = new FutureParser();
                parser.setParser(body.asParser());
            }
            return parser;
        }
    }

    public class NamedDefinition extends DelayedDefinition {
        String name;
        Scoped scoped = null;

        public NamedDefinition(String name) {
            this.name = name;
        }

        @Override
        public ParserCombinator asParser() {
            if (scoped == null) {
                scoped = new Scoped(FluentGrammar.this, name, PUBLIC, true);
                scoped.setParser(super.asParser());
            }
            return scoped;
        }
    }

}
