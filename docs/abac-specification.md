# RDF-ABAC : Specification

* [Label Syntax](#label-syntax)
* [Transport](#transport)
  * [Transport by header](#transport-by-header)
  * [Transport by RDF](#transport-in-rdf)
* [Evaluation](#evaluation)

## Label Syntax

Concepts:
* Attribute - a name
* Attribute-value - a pair of an attribute and a value
* Attribute expression - a boolean value expression where attributes
  are used to identify values in the curren request context.
* Attribute expression list - 
  a comma-separated list of attribute expression
* Attribute Value list  - 
  a comma-separated list of attribute-values.

### Syntax

An attribute is a sequence of Unicode characters.

It is written as either as a string value (in quotes) or as a word (no
quotes).

It does not matter how the attribute is written. `abc` and `"abc"` are the same attribute.
The string form is more general; a bare word has some syntax restrictions.

Strings can be written with single or double quotes; it does not matter which is
used except a string must start and finish with the same kind of quote.

Strings may include escaped characters (same a RDF Turtle): `\t`, `\n`, `\\\\` and unicode
escapes such as `\u1234` and `\U12345678`.

A string can be any sequence of characters, including an empty string, and
strings with spaces in them which can not be writtern as word labels.

Examples:
```
label
"label"
'label'
"one attribute"
``` 

### Syntax of Words

* A word is one or more characters.
* It is composed of characters which are alphanumeric or one of `_`, `:`, `.`, `-`, `+`.
* It must start with a letter or with `_` (not `:`, `.`, `-`, `+`).
* It must not end with a letter or with `_` (not `:`, `.`, `-`, `+`).
* Is not a keyword (`true` or `false`)

```
word ::= WordStartChar ( WordMiddleChar* WordLastChar )?
WordStartChar   ::= ( AZN | '_' )
WordMiddleChar  ::= ( AZN | '_' | ':' | '.' | '-' | '+' )
WordLastChar    ::= ( AZN | '_' )

AZN ::= (Unicode alphabetic|['0'-'9'])
```

### Attribute Expressions

Data is labelled with an attribute expressions.

Example expressions:
```
word
"string"
country=uk & employee
country=us & ( employee | contractor)
classification < secret
classification < "quite secret"
```

Grammar:
```
        Expr = ExprOr | "*" | "!"
        ExprOr = ExprAnd ("|" ExprOr)*
        ExprAnd = ExprRel ("&" ExprAnd) *
        ExprRel = Bracketted | Attr (RE ValueTerm)?
        Attr = (AZN|[_])(AZN|[_:.-+])*(AZN|[_])? | quotedString
        ValueTerm = Attr or number
        RE = oneof "=", "==", "<", ">", "!="
```
`=` and `==` are the same, the equality relationship.

AND is written `&` or `&&`. 

OR is written `|` or `||`. 

Parentheses are allowed.

Precedence: 
`A&B|C&D` is equivalent to `(A & (B|C) & D)`.

(think of as: "&" is + (addition) and "|" is * (multiply)).

### Special Attributee Expressions

An attribute expression of "!" means "deny" (i.e. evaluate to false)
and of "*" means "allow" (evaluates to true).
These elements can not be part of a larger expression.

### Attribute Values

Attribute values are written as "attribute = value"

Example attribute values: an attribute on its own is equavlent to "attribute = true"
```
    classification = general
```

Grammar:
```
    AttributeValue = Attr ('=' ValueTerm)?
```

### Lists

List of attribute expressions or attribute values are comma-separated lists. 

Example attribute expression list:
```
    classification = public , status != draft
```

Attribute value lists are used for the attributes a user request has. The list is used
as the "value set" in attribute expression evaluation.

In JSON, an attribute list is a JSON array, with each member being a JSON string
which is a attribute value with the full range of characters.

An attribute list can be empty.

### Attribute Expression Evaluation 

Example:

Given a value set of "abc=true" , "def=published". This is the same as: "abc" , "def=published".

`abc` evaluates to true.

`xyz` evaluates to false - `xyz` is absent from the attribute value set

`abc || xyz` evaluates to true

`abc && xyz` evaluates to false

`*` evaluates to true.

`!` evaluates to false.

`def` evaluates to false because `def` ha svalue "published", not `true`.

## Transport

### Default Attribute Expressions

As a convenience, we allow attribute lists as the default labelling header for data
events as well as an attribute expression.

### Transport By Header

One way to apply labels is to place it in the header of the update request. Headers
are supported for Kafka events and for HTTP operations.

The header name is `Security-Label` and has an attribute expression list.

A list of labels is treated as an AND (that is, to access the data, the user making the request will need to have all the attributes).

A list of labels is treated as an AND (that is, to access the data, the user making
the request will need to have all the attributes).

```
Security-Label: attribute1 , attribute2="some value"
```

An expression can be used - it will need to be enclosed in `" "` (HTTP field rules)
and strings use `' '`.

```
Security-Label: "attribute1 && attribute2 = 'some value'"
```

### Transport In RDF

Data can be added to an RDF graph with an upload consisting of the triples data and
labels. The system uses [TriG format](https://www.w3.org/TR/trig) for a
collection of graphs for data upload. We use this format to separate the data to
be added to the dataset default graph from the labels that controls access to
the data. By using TriG we still have a single unit to load so data and labels
get added in a single transactional change to the data.

The labels go in the graph named `policy:labels` (full form
`http://telicent.io/security#labels`). Graphs in the names being
`http://telicent.io/security#` are reserved for ABAC information and can not be
inserted into the visible data.

Example data upload:

```
PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ​
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>​
PREFIX authz:  <http://telicent.io/security#>​
PREFIX ANY     <jena:ANY>​
PREFIX :        <http://example/> ​
​
:person4321 rdf:type foaf:Person;​
    :phone "0400 111 222" ;​
    :phone "0400 111 333" ;​
    rdfs:label "Jones" ;​
    .​
​
GRAPH authz:labels {​
    ## Default :phone is "deny"​
    [ authz:pattern 'ANY :phone ANY' ;  authz:label "!" ] .​
    [ authz:pattern ':person4321 :phone "0400 111 333"' ;  authz:label "*" ] .​
    [ authz:pattern ':person4321 :phone "0400 111 222"' ;  authz:label "employee" ] .​
    [ authz:pattern ':person4321 ANY ANY' ;  authz:label "employee | contractor" ] .​
}​
```

Here, the triple `:person4321 :phone "0400 111 333` is visible to any user that
is permitted to use the query API.

The triple `:person4321 :phone "0400 111 222` is visible to any user with
attribute "employee".

Any other triple with propery `:phone` is not visible to any user.

## By Pattern

The label is a pattern and an attribute expression. The pattern describes the
triples in the data that the labelling applies to.
The label "!" means "deny" (never matches) and "*" means "allow" which always matches.

Labels can be given by pattern using `ANY` as a wildcard match.

```
    # Pattern:​ subject-property
    [ authz:pattern ':person4321 :phone ANY' ; authz:label   "employee" ] .

    # Pattern: Any use of the property.​
    [ authz:pattern 'ANY :phone ANY' ; authz:label "!" ] .

    # Pattern: subject
    [ authz:pattern ':person4321 ANY ANY' authz:label "employee" ] .
```

The pattern matching is currently limited to a pattern about the triple, not to triples
nearby such as the type of the subject.

Triple pattern matching defines the order of significance of multiple pattern
matches and the most significant match is the one used. Other matches are not
applied.

* `Concrete` - the triple being accessed
* `subject predicate ANY`
* `subject ANY ANY`
* `ANY predicate ANY`
* Dataset default label

## Evaluation
