---
title: JSON API
---

Besom comes with it's own JSON library to avoid any issues with classpath clashes with mainstream JSON libraries when embedding
Besom infrastructural programs in Scala applications via AutomationAPI. Package `besom.json` is a fork of well-known, battle-tested 
[spray-json](https://github.com/spray/spray-json) library. Specifically, the `spray-json` has been ported to Scala 3 with some 
breaking changes and received support for `derives` keyword. Another change is that import of `besom.json.*` brings all the 
`JsonFormat` instances from `DefaultJsonProtocol` into scope and to get the old experience to how `spray-json` operated one needs 
to import `besom.json.custom.*`. 

A sample use of the package:
```scala
import besom.json.*

case class Color(name: String, red: Int, green: Int, blue: Int = 160) derives JsonFormat
val color = Color("CadetBlue", 95, 158)

val json = """{"name":"CadetBlue","red":95,"green":158}"""

assert(color.toJson.convertTo[Color] == color)
assert(json.parseJson.convertTo[Color] == color)
```

#### JSON Interpolator

Besom-json package has also a convenient json interpolator that allows one to rewrite snippets like this
where cloud provider API expects a JSON string:

```scala
s3.BucketPolicyArgs(
  bucket = feedBucket.id,
  policy = JsObject(
    "Version" -> JsString("2012-10-17"),
    "Statement" -> JsArray(
      JsObject(
        "Sid" -> JsString("PublicReadGetObject"),
        "Effect" -> JsString("Allow"),
        "Principal" -> JsObject(
          "AWS" -> JsString("*")
        ),
        "Action" -> JsArray(JsString("s3:GetObject")),
        "Resource" -> JsArray(JsString(s"arn:aws:s3:::${name}/*"))
      )
    )
  ).prettyPrint
)
```

into simpler and less clunky interpolated variant like this: 

```scala
s3.BucketPolicyArgs(
  bucket = feedBucket.id,
  policy = json"""{
    "Version": "2012-10-17",
    "Statement": [{
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3::${name}/*"]
    }]
  }""".map(_.prettyPrint)
)
```

For ease of use with Besom Inputs in provider packages interpolator returns an `Output[JsValue]`.

The JSON interpolator is available when using `import besom.json.*` or `import besom.util.JsonInterpolator.*` imports. Interpolator is 
completely compile-time type safe and verifies JSON string for correctness by substituting . The only types that can be interpolated are 
`String`, `Int`, `Short`, `Long`, `Float`, `Double`, `JsValue` and `Option` and `Output` of the former (in whatever amount of nesting). 
If you need to interpolate a more complex type it's advised to derive a `JsonFormat` for it and convert it to `JsValue`.