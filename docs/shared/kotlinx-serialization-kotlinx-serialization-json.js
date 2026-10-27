(function (factory) {
  if (typeof define === 'function' && define.amd)
    define(['exports', './kotlinx-serialization-kotlinx-serialization-core.js', './kotlin-kotlin-stdlib.js'], factory);
  else if (typeof exports === 'object')
    factory(module.exports, require('./kotlinx-serialization-kotlinx-serialization-core.js'), require('./kotlin-kotlin-stdlib.js'));
  else {
    if (typeof globalThis['kotlinx-serialization-kotlinx-serialization-core'] === 'undefined') {
      throw new Error("Error loading module 'kotlinx-serialization-kotlinx-serialization-json'. Its dependency 'kotlinx-serialization-kotlinx-serialization-core' was not found. Please, check whether 'kotlinx-serialization-kotlinx-serialization-core' is loaded prior to 'kotlinx-serialization-kotlinx-serialization-json'.");
    }
    if (typeof globalThis['kotlin-kotlin-stdlib'] === 'undefined') {
      throw new Error("Error loading module 'kotlinx-serialization-kotlinx-serialization-json'. Its dependency 'kotlin-kotlin-stdlib' was not found. Please, check whether 'kotlin-kotlin-stdlib' is loaded prior to 'kotlinx-serialization-kotlinx-serialization-json'.");
    }
    globalThis['kotlinx-serialization-kotlinx-serialization-json'] = factory(typeof globalThis['kotlinx-serialization-kotlinx-serialization-json'] === 'undefined' ? {} : globalThis['kotlinx-serialization-kotlinx-serialization-json'], globalThis['kotlinx-serialization-kotlinx-serialization-core'], globalThis['kotlin-kotlin-stdlib']);
  }
}(function (_, kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core, kotlin_kotlin) {
  'use strict';
  //region block: imports
  var imul = Math.imul;
  var EmptySerializersModule = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.a2;
  var protoOf = kotlin_kotlin.$_$.a8;
  var initMetadataForObject = kotlin_kotlin.$_$.l7;
  var VOID = kotlin_kotlin.$_$.b;
  var Unit_instance = kotlin_kotlin.$_$.o2;
  var initMetadataForClass = kotlin_kotlin.$_$.g7;
  var toString = kotlin_kotlin.$_$.c8;
  var IllegalArgumentException_init_$Create$ = kotlin_kotlin.$_$.j3;
  var charSequenceLength = kotlin_kotlin.$_$.x6;
  var charSequenceGet = kotlin_kotlin.$_$.w6;
  var _Char___init__impl__6a9atx = kotlin_kotlin.$_$.d;
  var equals = kotlin_kotlin.$_$.b7;
  var toString_0 = kotlin_kotlin.$_$.ta;
  var Enum = kotlin_kotlin.$_$.r9;
  var Decoder = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.l1;
  var CompositeDecoder = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.j1;
  var initMetadataForInterface = kotlin_kotlin.$_$.j7;
  var initMetadataForCompanion = kotlin_kotlin.$_$.h7;
  var StringBuilder_init_$Create$ = kotlin_kotlin.$_$.f3;
  var hashCode = kotlin_kotlin.$_$.f7;
  var joinToString = kotlin_kotlin.$_$.b5;
  var THROW_CCE = kotlin_kotlin.$_$.x9;
  var KtMap = kotlin_kotlin.$_$.v3;
  var numberRangeToNumber = kotlin_kotlin.$_$.x7;
  var ClosedRange = kotlin_kotlin.$_$.d8;
  var isInterface = kotlin_kotlin.$_$.t7;
  var contains = kotlin_kotlin.$_$.g8;
  var convertToInt = kotlin_kotlin.$_$.e6;
  var SerializerFactory = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.v1;
  var getKClassFromExpression = kotlin_kotlin.$_$.k8;
  var getBooleanHashCode = kotlin_kotlin.$_$.c7;
  var getStringHashCode = kotlin_kotlin.$_$.e7;
  var KtList = kotlin_kotlin.$_$.t3;
  var toDouble = kotlin_kotlin.$_$.a9;
  var StringCompanionObject_instance = kotlin_kotlin.$_$.b2;
  var serializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.s;
  var InlinePrimitiveDescriptor = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.s1;
  var SerializableWith = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.g2;
  var SEALED_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.a;
  var buildSerialDescriptor = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.f1;
  var noWhenBranchMatchedException = kotlin_kotlin.$_$.ra;
  var KSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.e2;
  var STRING_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.b;
  var MapSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.q;
  var SerialDescriptor = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.d1;
  var ListSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.p;
  var ENUM_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.d;
  var PrimitiveSerialDescriptor = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.z;
  var toLongOrNull = kotlin_kotlin.$_$.d9;
  var toULongOrNull = kotlin_kotlin.$_$.g9;
  var ULong = kotlin_kotlin.$_$.ea;
  var Companion_getInstance = kotlin_kotlin.$_$.m2;
  var serializer_0 = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.w;
  var _ULong___get_data__impl__fggpzb = kotlin_kotlin.$_$.d1;
  var toDoubleOrNull = kotlin_kotlin.$_$.z8;
  var toBooleanStrictOrNull = kotlin_kotlin.$_$.y8;
  var IllegalStateException_init_$Create$ = kotlin_kotlin.$_$.l3;
  var KProperty1 = kotlin_kotlin.$_$.n8;
  var getPropertyCallableRef = kotlin_kotlin.$_$.d7;
  var constructCallableReference = kotlin_kotlin.$_$.z6;
  var lazy = kotlin_kotlin.$_$.qa;
  var get_isNullable = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.c1;
  var get_isInline = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.b1;
  var get_annotations = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.a1;
  var Encoder = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.m1;
  var CompositeEncoder = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.k1;
  var captureStack = kotlin_kotlin.$_$.t6;
  var SerializationException = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.h2;
  var SerializationException_init_$Init$ = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.n;
  var defineProp = kotlin_kotlin.$_$.a7;
  var fromInt = kotlin_kotlin.$_$.h6;
  var _UInt___init__impl__l7qpdl = kotlin_kotlin.$_$.t;
  var UInt__toString_impl_dbgl21 = kotlin_kotlin.$_$.v;
  var _ULong___init__impl__c78o9k = kotlin_kotlin.$_$.c1;
  var ULong__toString_impl_f9au7k = kotlin_kotlin.$_$.e1;
  var _UByte___init__impl__g9hnc4 = kotlin_kotlin.$_$.k;
  var UByte__toString_impl_v72jg = kotlin_kotlin.$_$.m;
  var _UShort___init__impl__jigrne = kotlin_kotlin.$_$.l1;
  var UShort__toString_impl_edaoee = kotlin_kotlin.$_$.n1;
  var ElementMarker = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.p1;
  var charSequenceSubSequence = kotlin_kotlin.$_$.y6;
  var coerceAtLeast = kotlin_kotlin.$_$.e8;
  var coerceAtMost = kotlin_kotlin.$_$.f8;
  var isBlank = kotlin_kotlin.$_$.t8;
  var Collection = kotlin_kotlin.$_$.o3;
  var SerializationException_init_$Create$ = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.m;
  var toSet = kotlin_kotlin.$_$.u5;
  var jsonCachedSerialNames = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.x1;
  var CLASS_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.e;
  var LinkedHashMap_init_$Create$ = kotlin_kotlin.$_$.z2;
  var ArrayList_init_$Create$ = kotlin_kotlin.$_$.q2;
  var singleOrNull = kotlin_kotlin.$_$.m5;
  var emptyMap = kotlin_kotlin.$_$.v4;
  var LinkedHashSet_init_$Create$ = kotlin_kotlin.$_$.c3;
  var getValue = kotlin_kotlin.$_$.x4;
  var copyOf = kotlin_kotlin.$_$.p4;
  var arrayCopy = kotlin_kotlin.$_$.c4;
  var LIST_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.f;
  var to = kotlin_kotlin.$_$.ua;
  var ENUM = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.e1;
  var PrimitiveKind = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.y;
  var PolymorphicKind = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.x;
  var CONTEXTUAL_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.c;
  var MAP_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.g;
  var contextual = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.b2;
  var SerializersModuleCollector = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.c2;
  var DeepRecursiveFunction = kotlin_kotlin.$_$.p9;
  var invoke = kotlin_kotlin.$_$.ma;
  var CoroutineImpl = kotlin_kotlin.$_$.y5;
  var DeepRecursiveScope = kotlin_kotlin.$_$.q9;
  var Unit = kotlin_kotlin.$_$.ha;
  var get_COROUTINE_SUSPENDED = kotlin_kotlin.$_$.x5;
  var initMetadataForLambda = kotlin_kotlin.$_$.k7;
  var initMetadataForCoroutine = kotlin_kotlin.$_$.i7;
  var enumEntries = kotlin_kotlin.$_$.z5;
  var getContextualDescriptor = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.g1;
  var AbstractDecoder = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.h1;
  var AbstractPolymorphicSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.n1;
  var DeserializationStrategy = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.d2;
  var getKClass = kotlin_kotlin.$_$.l8;
  var Exception = kotlin_kotlin.$_$.s9;
  var ensureNotNull = kotlin_kotlin.$_$.la;
  var contains_0 = kotlin_kotlin.$_$.p8;
  var plus = kotlin_kotlin.$_$.sa;
  var missingFieldExceptionWithNewMessage = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.y1;
  var MissingFieldException = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.f2;
  var convertToByte = kotlin_kotlin.$_$.d6;
  var equalsLong = kotlin_kotlin.$_$.g6;
  var convertToShort = kotlin_kotlin.$_$.f6;
  var IllegalArgumentException = kotlin_kotlin.$_$.t9;
  var isFinite = kotlin_kotlin.$_$.oa;
  var isFinite_0 = kotlin_kotlin.$_$.na;
  var charCodeAt = kotlin_kotlin.$_$.v6;
  var toUInt = kotlin_kotlin.$_$.f9;
  var _UInt___get_data__impl__f0vqqw = kotlin_kotlin.$_$.u;
  var toULong = kotlin_kotlin.$_$.h9;
  var toUByte = kotlin_kotlin.$_$.e9;
  var _UByte___get_data__impl__jof9qr = kotlin_kotlin.$_$.l;
  var toUShort = kotlin_kotlin.$_$.i9;
  var _UShort___get_data__impl__g0245 = kotlin_kotlin.$_$.m1;
  var objectCreate = kotlin_kotlin.$_$.z7;
  var AbstractEncoder = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.i1;
  var OBJECT_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.h;
  var SerializationStrategy = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.i2;
  var toString_1 = kotlin_kotlin.$_$.g;
  var Companion_getInstance_0 = kotlin_kotlin.$_$.l2;
  var serializer_1 = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.u;
  var Companion_getInstance_1 = kotlin_kotlin.$_$.k2;
  var serializer_2 = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.v;
  var Companion_getInstance_2 = kotlin_kotlin.$_$.n2;
  var serializer_3 = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.t;
  var setOf = kotlin_kotlin.$_$.l5;
  var Char__toInt_impl_vasixd = kotlin_kotlin.$_$.f;
  var numberToChar = kotlin_kotlin.$_$.y7;
  var equals_0 = kotlin_kotlin.$_$.q8;
  var toByte = kotlin_kotlin.$_$.b8;
  var startsWith = kotlin_kotlin.$_$.w8;
  var NamedValueDecoder = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.t1;
  var single = kotlin_kotlin.$_$.v8;
  var Char = kotlin_kotlin.$_$.n9;
  var toInt = kotlin_kotlin.$_$.c9;
  var toList = kotlin_kotlin.$_$.q5;
  var toNumber = kotlin_kotlin.$_$.p6;
  var last = kotlin_kotlin.$_$.f5;
  var removeLast = kotlin_kotlin.$_$.k5;
  var lastIndexOf = kotlin_kotlin.$_$.u8;
  var Long = kotlin_kotlin.$_$.u9;
  var Char__minus_impl_a2frrh = kotlin_kotlin.$_$.e;
  var multiply = kotlin_kotlin.$_$.k6;
  var add = kotlin_kotlin.$_$.a6;
  var subtract = kotlin_kotlin.$_$.o6;
  var compare = kotlin_kotlin.$_$.c6;
  var numberToLong = kotlin_kotlin.$_$.m6;
  var negate = kotlin_kotlin.$_$.l6;
  var charArray = kotlin_kotlin.$_$.u6;
  var indexOf = kotlin_kotlin.$_$.r8;
  var indexOf_0 = kotlin_kotlin.$_$.s8;
  var substring = kotlin_kotlin.$_$.x8;
  var StringBuilder_init_$Create$_0 = kotlin_kotlin.$_$.e3;
  var HashMap_init_$Create$ = kotlin_kotlin.$_$.s2;
  //endregion
  //region block: pre-declaration
  initMetadataForClass(Json, 'Json');
  initMetadataForObject(Default, 'Default', VOID, Json);
  initMetadataForClass(JsonBuilder, 'JsonBuilder');
  initMetadataForClass(JsonImpl, 'JsonImpl', VOID, Json);
  initMetadataForClass(JsonClassDiscriminator, 'JsonClassDiscriminator');
  initMetadataForClass(JsonIgnoreUnknownKeys, 'JsonIgnoreUnknownKeys');
  initMetadataForClass(JsonNames, 'JsonNames');
  initMetadataForClass(JsonConfiguration, 'JsonConfiguration');
  initMetadataForClass(ClassDiscriminatorMode, 'ClassDiscriminatorMode', VOID, Enum);
  initMetadataForInterface(JsonDecoder, 'JsonDecoder', VOID, VOID, [Decoder, CompositeDecoder]);
  initMetadataForCompanion(Companion);
  initMetadataForClass(JsonElement, 'JsonElement', VOID, VOID, VOID, VOID, VOID, {0: JsonElementSerializer_getInstance});
  initMetadataForCompanion(Companion_0);
  initMetadataForClass(JsonPrimitive, 'JsonPrimitive', VOID, JsonElement, VOID, VOID, VOID, {0: JsonPrimitiveSerializer_getInstance});
  initMetadataForCompanion(Companion_1);
  initMetadataForClass(JsonObject, 'JsonObject', VOID, JsonElement, [KtMap], VOID, VOID, {0: JsonObjectSerializer_getInstance});
  initMetadataForObject(JsonNull, 'JsonNull', VOID, JsonPrimitive, [SerializerFactory], VOID, VOID, {0: JsonNullSerializer_getInstance});
  initMetadataForClass(JsonLiteral, 'JsonLiteral', VOID, JsonPrimitive);
  initMetadataForCompanion(Companion_2);
  initMetadataForClass(JsonArray, 'JsonArray', VOID, JsonElement, [KtList], VOID, VOID, {0: JsonArraySerializer_getInstance});
  initMetadataForObject(JsonElementSerializer, 'JsonElementSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(JsonPrimitiveSerializer, 'JsonPrimitiveSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(JsonObjectDescriptor, 'JsonObjectDescriptor', VOID, VOID, [SerialDescriptor]);
  initMetadataForObject(JsonObjectSerializer, 'JsonObjectSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(JsonArrayDescriptor, 'JsonArrayDescriptor', VOID, VOID, [SerialDescriptor]);
  initMetadataForObject(JsonArraySerializer, 'JsonArraySerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(JsonNullSerializer, 'JsonNullSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(JsonLiteralSerializer, 'JsonLiteralSerializer', VOID, VOID, [KSerializer]);
  initMetadataForClass(defer$1, VOID, VOID, VOID, [SerialDescriptor]);
  initMetadataForInterface(JsonEncoder, 'JsonEncoder', VOID, VOID, [Encoder, CompositeEncoder]);
  initMetadataForClass(JsonException, 'JsonException', VOID, SerializationException);
  initMetadataForClass(JsonDecodingException, 'JsonDecodingException', VOID, JsonException);
  initMetadataForClass(JsonEncodingException, 'JsonEncodingException', VOID, JsonException);
  initMetadataForClass(Composer, 'Composer');
  initMetadataForClass(ComposerForUnsignedNumbers, 'ComposerForUnsignedNumbers', VOID, Composer);
  initMetadataForClass(ComposerForUnquotedLiterals, 'ComposerForUnquotedLiterals', VOID, Composer);
  initMetadataForClass(ComposerWithPrettyPrint, 'ComposerWithPrettyPrint', VOID, Composer);
  initMetadataForClass(JsonElementMarker, 'JsonElementMarker');
  initMetadataForObject(Tombstone, 'Tombstone');
  initMetadataForObject(RedactedKey, 'RedactedKey');
  initMetadataForClass(JsonPath, 'JsonPath');
  initMetadataForClass(JsonSerializersModuleValidator, 'JsonSerializersModuleValidator', VOID, VOID, [SerializersModuleCollector]);
  initMetadataForLambda(JsonTreeReader$readDeepRecursive$slambda, CoroutineImpl, VOID, [2]);
  initMetadataForCoroutine($readObjectCOROUTINE$, CoroutineImpl);
  initMetadataForClass(JsonTreeReader, 'JsonTreeReader', VOID, VOID, VOID, [1]);
  initMetadataForClass(LexerMode, 'LexerMode', VOID, Enum);
  initMetadataForInterface(PolymorphicJsonDecoder, 'PolymorphicJsonDecoder', VOID, VOID, [JsonDecoder]);
  initMetadataForClass(Key, 'Key', Key);
  initMetadataForClass(DescriptorSchemaCache, 'DescriptorSchemaCache', DescriptorSchemaCache);
  initMetadataForClass(DiscriminatorHolder, 'DiscriminatorHolder');
  initMetadataForClass(StreamingJsonDecoder, 'StreamingJsonDecoder', VOID, AbstractDecoder, [PolymorphicJsonDecoder]);
  initMetadataForClass(JsonDecoderForUnsignedTypes, 'JsonDecoderForUnsignedTypes', VOID, AbstractDecoder);
  initMetadataForClass(StreamingJsonEncoder, 'StreamingJsonEncoder', VOID, AbstractEncoder, [JsonEncoder]);
  initMetadataForClass(AbstractJsonTreeDecoder, 'AbstractJsonTreeDecoder', VOID, NamedValueDecoder, [PolymorphicJsonDecoder]);
  initMetadataForClass(JsonTreeDecoder, 'JsonTreeDecoder', VOID, AbstractJsonTreeDecoder);
  initMetadataForClass(JsonTreeListDecoder, 'JsonTreeListDecoder', VOID, AbstractJsonTreeDecoder);
  initMetadataForClass(JsonPrimitiveDecoder, 'JsonPrimitiveDecoder', VOID, AbstractJsonTreeDecoder);
  initMetadataForClass(JsonTreeMapDecoder, 'JsonTreeMapDecoder', VOID, JsonTreeDecoder);
  initMetadataForClass(AbstractJsonLexer, 'AbstractJsonLexer');
  initMetadataForObject(CharMappings, 'CharMappings');
  initMetadataForClass(StringJsonLexer, 'StringJsonLexer', VOID, AbstractJsonLexer);
  initMetadataForClass(StringJsonLexerWithComments, 'StringJsonLexerWithComments', VOID, StringJsonLexer);
  initMetadataForClass(StringJsonWriter, 'StringJsonWriter', StringJsonWriter);
  //endregion
  function Default() {
    Default_instance = this;
    Json.call(this, new JsonConfiguration(), EmptySerializersModule());
  }
  var Default_instance;
  function Default_getInstance() {
    if (Default_instance == null)
      new Default();
    return Default_instance;
  }
  function Json(configuration, serializersModule) {
    Default_getInstance();
    this.iz_1 = configuration;
    this.jz_1 = serializersModule;
    this.kz_1 = new DescriptorSchemaCache();
  }
  protoOf(Json).fj = function () {
    return this.jz_1;
  };
  protoOf(Json).lz = function (serializer, value) {
    var result = new StringJsonWriter();
    try {
      encodeByWriter(this, result, serializer, value);
      return result.toString();
    }finally {
      result.qz();
    }
  };
  protoOf(Json).mz = function (deserializer, string) {
    var lexer = StringJsonLexer_0(this, string);
    var input = new StreamingJsonDecoder(this, LexerMode_OBJ_getInstance(), lexer, deserializer.of(), null);
    var result = input.pi(deserializer);
    lexer.e10();
    return result;
  };
  protoOf(Json).nz = function (deserializer, element) {
    return readJson(this, element, deserializer);
  };
  protoOf(Json).oz = function (string) {
    return this.mz(JsonElementSerializer_getInstance(), string);
  };
  function Json_0(from, builderAction) {
    from = from === VOID ? Default_getInstance() : from;
    var builder = new JsonBuilder(from);
    builderAction(builder);
    var conf = builder.z10();
    return new JsonImpl(conf, builder.w10_1);
  }
  function JsonBuilder(json) {
    this.f10_1 = json.iz_1.a11_1;
    this.g10_1 = json.iz_1.f11_1;
    this.h10_1 = json.iz_1.b11_1;
    this.i10_1 = json.iz_1.c11_1;
    this.j10_1 = json.iz_1.e11_1;
    this.k10_1 = json.iz_1.g11_1;
    this.l10_1 = json.iz_1.h11_1;
    this.m10_1 = json.iz_1.j11_1;
    this.n10_1 = json.iz_1.q11_1;
    this.o10_1 = json.iz_1.l11_1;
    this.p10_1 = json.iz_1.m11_1;
    this.q10_1 = json.iz_1.n11_1;
    this.r10_1 = json.iz_1.o11_1;
    this.s10_1 = json.iz_1.p11_1;
    this.t10_1 = json.iz_1.k11_1;
    this.u10_1 = json.iz_1.d11_1;
    this.v10_1 = json.iz_1.i11_1;
    this.w10_1 = json.fj();
    this.x10_1 = json.iz_1.r11_1;
    this.y10_1 = json.iz_1.s11_1;
  }
  protoOf(JsonBuilder).z10 = function () {
    if (this.v10_1) {
      // Inline function 'kotlin.require' call
      if (!(this.m10_1 === 'type')) {
        var message = 'Class discriminator should not be specified when array polymorphism is specified';
        throw IllegalArgumentException_init_$Create$(toString(message));
      }
      // Inline function 'kotlin.require' call
      if (!this.n10_1.equals(ClassDiscriminatorMode_POLYMORPHIC_getInstance())) {
        var message_0 = 'useArrayPolymorphism option can only be used if classDiscriminatorMode in a default POLYMORPHIC state.';
        throw IllegalArgumentException_init_$Create$(toString(message_0));
      }
    }
    if (!this.j10_1) {
      // Inline function 'kotlin.require' call
      if (!(this.k10_1 === '    ')) {
        var message_1 = 'Indent should not be specified when default printing mode is used';
        throw IllegalArgumentException_init_$Create$(toString(message_1));
      }
    } else if (!(this.k10_1 === '    ')) {
      var tmp0 = this.k10_1;
      var tmp$ret$6;
      $l$block: {
        // Inline function 'kotlin.text.all' call
        var inductionVariable = 0;
        while (inductionVariable < charSequenceLength(tmp0)) {
          var element = charSequenceGet(tmp0, inductionVariable);
          inductionVariable = inductionVariable + 1 | 0;
          if (!(element === _Char___init__impl__6a9atx(32) || element === _Char___init__impl__6a9atx(9) || element === _Char___init__impl__6a9atx(13) || element === _Char___init__impl__6a9atx(10))) {
            tmp$ret$6 = false;
            break $l$block;
          }
        }
        tmp$ret$6 = true;
      }
      var allWhitespaces = tmp$ret$6;
      // Inline function 'kotlin.require' call
      if (!allWhitespaces) {
        var message_2 = 'Only whitespace, tab, newline and carriage return are allowed as pretty print symbols. Had ' + this.k10_1;
        throw IllegalArgumentException_init_$Create$(toString(message_2));
      }
    }
    return new JsonConfiguration(this.f10_1, this.h10_1, this.i10_1, this.u10_1, this.j10_1, this.g10_1, this.k10_1, this.l10_1, this.v10_1, this.m10_1, this.t10_1, this.o10_1, this.p10_1, this.q10_1, this.r10_1, this.s10_1, this.n10_1, this.x10_1, this.y10_1);
  };
  function validateConfiguration($this) {
    if (equals($this.fj(), EmptySerializersModule()))
      return Unit_instance;
    var collector = new JsonSerializersModuleValidator($this.iz_1);
    $this.fj().qy(collector);
  }
  function JsonImpl(configuration, module_0) {
    Json.call(this, configuration, module_0);
    validateConfiguration(this);
  }
  function JsonClassDiscriminator() {
  }
  function JsonIgnoreUnknownKeys() {
  }
  function JsonNames() {
  }
  function JsonConfiguration(encodeDefaults, ignoreUnknownKeys, isLenient, allowStructuredMapKeys, prettyPrint, explicitNulls, prettyPrintIndent, coerceInputValues, useArrayPolymorphism, classDiscriminator, allowSpecialFloatingPointValues, useAlternativeNames, namingStrategy, decodeEnumsCaseInsensitive, allowTrailingComma, allowComments, classDiscriminatorMode, exceptionsWithDebugInfo, maxNestingDepth) {
    encodeDefaults = encodeDefaults === VOID ? false : encodeDefaults;
    ignoreUnknownKeys = ignoreUnknownKeys === VOID ? false : ignoreUnknownKeys;
    isLenient = isLenient === VOID ? false : isLenient;
    allowStructuredMapKeys = allowStructuredMapKeys === VOID ? false : allowStructuredMapKeys;
    prettyPrint = prettyPrint === VOID ? false : prettyPrint;
    explicitNulls = explicitNulls === VOID ? true : explicitNulls;
    prettyPrintIndent = prettyPrintIndent === VOID ? '    ' : prettyPrintIndent;
    coerceInputValues = coerceInputValues === VOID ? false : coerceInputValues;
    useArrayPolymorphism = useArrayPolymorphism === VOID ? false : useArrayPolymorphism;
    classDiscriminator = classDiscriminator === VOID ? 'type' : classDiscriminator;
    allowSpecialFloatingPointValues = allowSpecialFloatingPointValues === VOID ? false : allowSpecialFloatingPointValues;
    useAlternativeNames = useAlternativeNames === VOID ? true : useAlternativeNames;
    namingStrategy = namingStrategy === VOID ? null : namingStrategy;
    decodeEnumsCaseInsensitive = decodeEnumsCaseInsensitive === VOID ? false : decodeEnumsCaseInsensitive;
    allowTrailingComma = allowTrailingComma === VOID ? false : allowTrailingComma;
    allowComments = allowComments === VOID ? false : allowComments;
    classDiscriminatorMode = classDiscriminatorMode === VOID ? ClassDiscriminatorMode_POLYMORPHIC_getInstance() : classDiscriminatorMode;
    exceptionsWithDebugInfo = exceptionsWithDebugInfo === VOID ? true : exceptionsWithDebugInfo;
    maxNestingDepth = maxNestingDepth === VOID ? 400 : maxNestingDepth;
    this.a11_1 = encodeDefaults;
    this.b11_1 = ignoreUnknownKeys;
    this.c11_1 = isLenient;
    this.d11_1 = allowStructuredMapKeys;
    this.e11_1 = prettyPrint;
    this.f11_1 = explicitNulls;
    this.g11_1 = prettyPrintIndent;
    this.h11_1 = coerceInputValues;
    this.i11_1 = useArrayPolymorphism;
    this.j11_1 = classDiscriminator;
    this.k11_1 = allowSpecialFloatingPointValues;
    this.l11_1 = useAlternativeNames;
    this.m11_1 = namingStrategy;
    this.n11_1 = decodeEnumsCaseInsensitive;
    this.o11_1 = allowTrailingComma;
    this.p11_1 = allowComments;
    this.q11_1 = classDiscriminatorMode;
    this.r11_1 = exceptionsWithDebugInfo;
    this.s11_1 = maxNestingDepth;
  }
  protoOf(JsonConfiguration).toString = function () {
    return 'JsonConfiguration(encodeDefaults=' + this.a11_1 + ', ignoreUnknownKeys=' + this.b11_1 + ', isLenient=' + this.c11_1 + ', ' + ('allowStructuredMapKeys=' + this.d11_1 + ', prettyPrint=' + this.e11_1 + ', explicitNulls=' + this.f11_1 + ', ') + ("prettyPrintIndent='" + this.g11_1 + "', coerceInputValues=" + this.h11_1 + ', useArrayPolymorphism=' + this.i11_1 + ', ') + ("classDiscriminator='" + this.j11_1 + "', allowSpecialFloatingPointValues=" + this.k11_1 + ', ') + ('useAlternativeNames=' + this.l11_1 + ', namingStrategy=' + toString_0(this.m11_1) + ', decodeEnumsCaseInsensitive=' + this.n11_1 + ', ') + ('allowTrailingComma=' + this.o11_1 + ', allowComments=' + this.p11_1 + ', classDiscriminatorMode=' + this.q11_1.toString() + ', ') + ('exceptionsWithDebugInfo=' + this.r11_1 + ', maxNestingDepth=' + this.s11_1 + ')');
  };
  var static_init_called;
  function static_init() {
    if (static_init_called)
      return Unit_instance;
    static_init_called = true;
    ClassDiscriminatorMode_NONE_instance = new ClassDiscriminatorMode('NONE', 0);
    ClassDiscriminatorMode_ALL_JSON_OBJECTS_instance = new ClassDiscriminatorMode('ALL_JSON_OBJECTS', 1);
    ClassDiscriminatorMode_POLYMORPHIC_instance = new ClassDiscriminatorMode('POLYMORPHIC', 2);
  }
  var ClassDiscriminatorMode_NONE_instance;
  var ClassDiscriminatorMode_ALL_JSON_OBJECTS_instance;
  var ClassDiscriminatorMode_POLYMORPHIC_instance;
  function ClassDiscriminatorMode(name, ordinal) {
    Enum.call(this, name, ordinal);
  }
  function ClassDiscriminatorMode_NONE_getInstance() {
    static_init();
    return ClassDiscriminatorMode_NONE_instance;
  }
  function ClassDiscriminatorMode_ALL_JSON_OBJECTS_getInstance() {
    static_init();
    return ClassDiscriminatorMode_ALL_JSON_OBJECTS_instance;
  }
  function ClassDiscriminatorMode_POLYMORPHIC_getInstance() {
    static_init();
    return ClassDiscriminatorMode_POLYMORPHIC_instance;
  }
  function JsonDecoder() {
  }
  function get_jsonUnquotedLiteralDescriptor() {
    _init_properties_JsonElement_kt__7cbdc2();
    return jsonUnquotedLiteralDescriptor;
  }
  var jsonUnquotedLiteralDescriptor;
  function Companion() {
  }
  var Companion_instance;
  function Companion_getInstance_3() {
    return Companion_instance;
  }
  function JsonElement() {
  }
  function Companion_0() {
  }
  var Companion_instance_0;
  function Companion_getInstance_4() {
    return Companion_instance_0;
  }
  function JsonPrimitive() {
    JsonElement.call(this);
  }
  protoOf(JsonPrimitive).toString = function () {
    return this.v11();
  };
  function Companion_1() {
  }
  var Companion_instance_1;
  function Companion_getInstance_5() {
    return Companion_instance_1;
  }
  function JsonObject$toString$lambda(_destruct__k2r9zo) {
    // Inline function 'kotlin.collections.component1' call
    var k = _destruct__k2r9zo.x1();
    // Inline function 'kotlin.collections.component2' call
    var v = _destruct__k2r9zo.y1();
    // Inline function 'kotlin.text.buildString' call
    // Inline function 'kotlin.apply' call
    var this_0 = StringBuilder_init_$Create$();
    printQuoted(this_0, k);
    this_0.q(_Char___init__impl__6a9atx(58));
    this_0.u(v);
    return this_0.toString();
  }
  function JsonObject(content) {
    JsonElement.call(this);
    this.w11_1 = content;
  }
  protoOf(JsonObject).equals = function (other) {
    return equals(this.w11_1, other);
  };
  protoOf(JsonObject).hashCode = function () {
    return hashCode(this.w11_1);
  };
  protoOf(JsonObject).toString = function () {
    var tmp = this.w11_1.e2();
    return joinToString(tmp, ',', '{', '}', VOID, VOID, JsonObject$toString$lambda);
  };
  protoOf(JsonObject).p = function () {
    return this.w11_1.p();
  };
  protoOf(JsonObject).x11 = function (key) {
    return this.w11_1.z1(key);
  };
  protoOf(JsonObject).z1 = function (key) {
    if (!(!(key == null) ? typeof key === 'string' : false))
      return false;
    return this.x11((!(key == null) ? typeof key === 'string' : false) ? key : THROW_CCE());
  };
  protoOf(JsonObject).y11 = function (key) {
    return this.w11_1.b2(key);
  };
  protoOf(JsonObject).b2 = function (key) {
    if (!(!(key == null) ? typeof key === 'string' : false))
      return null;
    return this.y11((!(key == null) ? typeof key === 'string' : false) ? key : THROW_CCE());
  };
  protoOf(JsonObject).x = function () {
    return this.w11_1.x();
  };
  protoOf(JsonObject).c2 = function () {
    return this.w11_1.c2();
  };
  protoOf(JsonObject).d2 = function () {
    return this.w11_1.d2();
  };
  protoOf(JsonObject).e2 = function () {
    return this.w11_1.e2();
  };
  function get_intOrNull(_this__u8e3s4) {
    _init_properties_JsonElement_kt__7cbdc2();
    // Inline function 'kotlinx.serialization.json.exceptionToNull' call
    var tmp;
    try {
      tmp = parseLongImpl(_this__u8e3s4);
    } catch ($p) {
      var tmp_0;
      if ($p instanceof JsonDecodingException) {
        var e = $p;
        tmp_0 = null;
      } else {
        throw $p;
      }
      tmp = tmp_0;
    }
    var tmp0_elvis_lhs = tmp;
    var tmp_1;
    if (tmp0_elvis_lhs == null) {
      return null;
    } else {
      tmp_1 = tmp0_elvis_lhs;
    }
    var result = tmp_1;
    // Inline function 'kotlin.ranges.contains' call
    var this_0 = numberRangeToNumber(-2147483648, 2147483647);
    if (!contains(isInterface(this_0, ClosedRange) ? this_0 : THROW_CCE(), result))
      return null;
    return convertToInt(result);
  }
  function parseLongImpl(_this__u8e3s4) {
    _init_properties_JsonElement_kt__7cbdc2();
    return StringJsonLexer_0(Default_getInstance(), _this__u8e3s4.v11()).z11();
  }
  function get_jsonPrimitive(_this__u8e3s4) {
    _init_properties_JsonElement_kt__7cbdc2();
    var tmp0_elvis_lhs = _this__u8e3s4 instanceof JsonPrimitive ? _this__u8e3s4 : null;
    var tmp;
    if (tmp0_elvis_lhs == null) {
      error(_this__u8e3s4, 'JsonPrimitive');
    } else {
      tmp = tmp0_elvis_lhs;
    }
    return tmp;
  }
  function get_contentOrNull(_this__u8e3s4) {
    _init_properties_JsonElement_kt__7cbdc2();
    var tmp;
    if (_this__u8e3s4 instanceof JsonNull) {
      tmp = null;
    } else {
      tmp = _this__u8e3s4.v11();
    }
    return tmp;
  }
  function JsonNull() {
    JsonNull_instance = this;
    JsonPrimitive.call(this);
    this.a12_1 = 'null';
  }
  protoOf(JsonNull).v11 = function () {
    return this.a12_1;
  };
  protoOf(JsonNull).b12 = function () {
    return JsonNullSerializer_getInstance();
  };
  protoOf(JsonNull).br = function (typeParamsSerializers) {
    return this.b12();
  };
  var JsonNull_instance;
  function JsonNull_getInstance() {
    if (JsonNull_instance == null)
      new JsonNull();
    return JsonNull_instance;
  }
  function JsonPrimitive_0(value) {
    _init_properties_JsonElement_kt__7cbdc2();
    if (value == null)
      return JsonNull_getInstance();
    return new JsonLiteral(value, true);
  }
  function JsonLiteral(body, isString, coerceToInlineType) {
    coerceToInlineType = coerceToInlineType === VOID ? null : coerceToInlineType;
    JsonPrimitive.call(this);
    this.c12_1 = isString;
    this.d12_1 = coerceToInlineType;
    this.e12_1 = toString(body);
    if (!(this.d12_1 == null)) {
      // Inline function 'kotlin.require' call
      // Inline function 'kotlin.require' call
      if (!this.d12_1.vg()) {
        var message = 'Failed requirement.';
        throw IllegalArgumentException_init_$Create$(toString(message));
      }
    }
  }
  protoOf(JsonLiteral).v11 = function () {
    return this.e12_1;
  };
  protoOf(JsonLiteral).toString = function () {
    var tmp;
    if (this.c12_1) {
      // Inline function 'kotlin.text.buildString' call
      // Inline function 'kotlin.apply' call
      var this_0 = StringBuilder_init_$Create$();
      printQuoted(this_0, this.e12_1);
      tmp = this_0.toString();
    } else {
      tmp = this.e12_1;
    }
    return tmp;
  };
  protoOf(JsonLiteral).equals = function (other) {
    if (this === other)
      return true;
    if (other == null || !getKClassFromExpression(this).equals(getKClassFromExpression(other)))
      return false;
    if (!(other instanceof JsonLiteral))
      THROW_CCE();
    if (!(this.c12_1 === other.c12_1))
      return false;
    if (!(this.e12_1 === other.e12_1))
      return false;
    return true;
  };
  protoOf(JsonLiteral).hashCode = function () {
    var result = getBooleanHashCode(this.c12_1);
    result = imul(31, result) + getStringHashCode(this.e12_1) | 0;
    return result;
  };
  function Companion_2() {
  }
  var Companion_instance_2;
  function Companion_getInstance_6() {
    return Companion_instance_2;
  }
  function JsonArray(content) {
    JsonElement.call(this);
    this.f12_1 = content;
  }
  protoOf(JsonArray).equals = function (other) {
    return equals(this.f12_1, other);
  };
  protoOf(JsonArray).hashCode = function () {
    return hashCode(this.f12_1);
  };
  protoOf(JsonArray).toString = function () {
    return joinToString(this.f12_1, ',', '[', ']');
  };
  protoOf(JsonArray).p = function () {
    return this.f12_1.p();
  };
  protoOf(JsonArray).r = function () {
    return this.f12_1.r();
  };
  protoOf(JsonArray).y = function (index) {
    return this.f12_1.y(index);
  };
  protoOf(JsonArray).x = function () {
    return this.f12_1.x();
  };
  function get_booleanOrNull(_this__u8e3s4) {
    _init_properties_JsonElement_kt__7cbdc2();
    return toBooleanStrictOrNull_0(_this__u8e3s4.v11());
  }
  function get_float(_this__u8e3s4) {
    _init_properties_JsonElement_kt__7cbdc2();
    // Inline function 'kotlin.text.toFloat' call
    var this_0 = _this__u8e3s4.v11();
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return toDouble(this_0);
  }
  function get_double(_this__u8e3s4) {
    _init_properties_JsonElement_kt__7cbdc2();
    return toDouble(_this__u8e3s4.v11());
  }
  function error(_this__u8e3s4, element) {
    _init_properties_JsonElement_kt__7cbdc2();
    throw IllegalArgumentException_init_$Create$('Element ' + toString(getKClassFromExpression(_this__u8e3s4)) + ' is not a ' + element);
  }
  var properties_initialized_JsonElement_kt_abxy8s;
  function _init_properties_JsonElement_kt__7cbdc2() {
    if (!properties_initialized_JsonElement_kt_abxy8s) {
      properties_initialized_JsonElement_kt_abxy8s = true;
      jsonUnquotedLiteralDescriptor = InlinePrimitiveDescriptor('kotlinx.serialization.json.JsonUnquotedLiteral', serializer(StringCompanionObject_instance));
    }
  }
  function JsonElementSerializer$descriptor$lambda($this$buildSerialDescriptor) {
    $this$buildSerialDescriptor.zf('JsonPrimitive', defer(JsonElementSerializer$descriptor$lambda$lambda));
    $this$buildSerialDescriptor.zf('JsonNull', defer(JsonElementSerializer$descriptor$lambda$lambda_0));
    $this$buildSerialDescriptor.zf('JsonLiteral', defer(JsonElementSerializer$descriptor$lambda$lambda_1));
    $this$buildSerialDescriptor.zf('JsonObject', defer(JsonElementSerializer$descriptor$lambda$lambda_2));
    $this$buildSerialDescriptor.zf('JsonArray', defer(JsonElementSerializer$descriptor$lambda$lambda_3));
    return Unit_instance;
  }
  function JsonElementSerializer$descriptor$lambda$lambda() {
    return JsonPrimitiveSerializer_getInstance().g12_1;
  }
  function JsonElementSerializer$descriptor$lambda$lambda_0() {
    return JsonNullSerializer_getInstance().h12_1;
  }
  function JsonElementSerializer$descriptor$lambda$lambda_1() {
    return JsonLiteralSerializer_getInstance().i12_1;
  }
  function JsonElementSerializer$descriptor$lambda$lambda_2() {
    return JsonObjectSerializer_getInstance().j12_1;
  }
  function JsonElementSerializer$descriptor$lambda$lambda_3() {
    return JsonArraySerializer_getInstance().k12_1;
  }
  function JsonElementSerializer() {
    JsonElementSerializer_instance = this;
    var tmp = this;
    var tmp_0 = SEALED_getInstance();
    tmp.l12_1 = buildSerialDescriptor('kotlinx.serialization.json.JsonElement', tmp_0, [], JsonElementSerializer$descriptor$lambda);
  }
  protoOf(JsonElementSerializer).of = function () {
    return this.l12_1;
  };
  protoOf(JsonElementSerializer).m12 = function (encoder, value) {
    verify(encoder);
    if (value instanceof JsonPrimitive) {
      encoder.ik(JsonPrimitiveSerializer_getInstance(), value);
    } else {
      if (value instanceof JsonObject) {
        encoder.ik(JsonObjectSerializer_getInstance(), value);
      } else {
        if (value instanceof JsonArray) {
          encoder.ik(JsonArraySerializer_getInstance(), value);
        } else {
          noWhenBranchMatchedException();
        }
      }
    }
  };
  protoOf(JsonElementSerializer).pf = function (encoder, value) {
    return this.m12(encoder, value instanceof JsonElement ? value : THROW_CCE());
  };
  protoOf(JsonElementSerializer).qf = function (decoder) {
    var input = asJsonDecoder(decoder);
    return input.u11();
  };
  var JsonElementSerializer_instance;
  function JsonElementSerializer_getInstance() {
    if (JsonElementSerializer_instance == null)
      new JsonElementSerializer();
    return JsonElementSerializer_instance;
  }
  function JsonPrimitiveSerializer() {
    JsonPrimitiveSerializer_instance = this;
    this.g12_1 = buildSerialDescriptor('kotlinx.serialization.json.JsonPrimitive', STRING_getInstance(), []);
  }
  protoOf(JsonPrimitiveSerializer).of = function () {
    return this.g12_1;
  };
  protoOf(JsonPrimitiveSerializer).n12 = function (encoder, value) {
    verify(encoder);
    var tmp;
    if (value instanceof JsonNull) {
      encoder.ik(JsonNullSerializer_getInstance(), JsonNull_getInstance());
      tmp = Unit_instance;
    } else {
      var tmp_0 = JsonLiteralSerializer_getInstance();
      encoder.ik(tmp_0, value instanceof JsonLiteral ? value : THROW_CCE());
      tmp = Unit_instance;
    }
    return tmp;
  };
  protoOf(JsonPrimitiveSerializer).pf = function (encoder, value) {
    return this.n12(encoder, value instanceof JsonPrimitive ? value : THROW_CCE());
  };
  protoOf(JsonPrimitiveSerializer).qf = function (decoder) {
    var jsonDecoder = asJsonDecoder(decoder);
    var result = jsonDecoder.u11();
    if (!(result instanceof JsonPrimitive)) {
      // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
      var shortMessage = 'Unexpected JSON element, expected JsonPrimitive, had ' + toString(getKClassFromExpression(result));
      var tmp;
      if (jsonDecoder.t11().iz_1.r11_1) {
        var tmp$ret$3 = toString(result);
        tmp = toString(minify(tmp$ret$3));
      } else {
        tmp = null;
      }
      var inputValue = tmp;
      throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, shortMessage, null, null, inputValue), shortMessage, -1, null, inputValue, null);
    }
    return result;
  };
  var JsonPrimitiveSerializer_instance;
  function JsonPrimitiveSerializer_getInstance() {
    if (JsonPrimitiveSerializer_instance == null)
      new JsonPrimitiveSerializer();
    return JsonPrimitiveSerializer_instance;
  }
  function JsonObjectDescriptor() {
    JsonObjectDescriptor_instance = this;
    this.o12_1 = MapSerializer(serializer(StringCompanionObject_instance), JsonElementSerializer_getInstance()).of();
    this.p12_1 = 'kotlinx.serialization.json.JsonObject';
  }
  protoOf(JsonObjectDescriptor).tg = function () {
    return this.p12_1;
  };
  protoOf(JsonObjectDescriptor).yg = function (index) {
    return this.o12_1.yg(index);
  };
  protoOf(JsonObjectDescriptor).zg = function (name) {
    return this.o12_1.zg(name);
  };
  protoOf(JsonObjectDescriptor).ah = function (index) {
    return this.o12_1.ah(index);
  };
  protoOf(JsonObjectDescriptor).bh = function (index) {
    return this.o12_1.bh(index);
  };
  protoOf(JsonObjectDescriptor).ch = function (index) {
    return this.o12_1.ch(index);
  };
  protoOf(JsonObjectDescriptor).ug = function () {
    return this.o12_1.ug();
  };
  protoOf(JsonObjectDescriptor).pg = function () {
    return this.o12_1.pg();
  };
  protoOf(JsonObjectDescriptor).vg = function () {
    return this.o12_1.vg();
  };
  protoOf(JsonObjectDescriptor).wg = function () {
    return this.o12_1.wg();
  };
  protoOf(JsonObjectDescriptor).xg = function () {
    return this.o12_1.xg();
  };
  var JsonObjectDescriptor_instance;
  function JsonObjectDescriptor_getInstance() {
    if (JsonObjectDescriptor_instance == null)
      new JsonObjectDescriptor();
    return JsonObjectDescriptor_instance;
  }
  function JsonObjectSerializer() {
    JsonObjectSerializer_instance = this;
    this.j12_1 = JsonObjectDescriptor_getInstance();
  }
  protoOf(JsonObjectSerializer).of = function () {
    return this.j12_1;
  };
  protoOf(JsonObjectSerializer).q12 = function (encoder, value) {
    verify(encoder);
    MapSerializer(serializer(StringCompanionObject_instance), JsonElementSerializer_getInstance()).pf(encoder, value);
  };
  protoOf(JsonObjectSerializer).pf = function (encoder, value) {
    return this.q12(encoder, value instanceof JsonObject ? value : THROW_CCE());
  };
  protoOf(JsonObjectSerializer).qf = function (decoder) {
    verify_0(decoder);
    return new JsonObject(MapSerializer(serializer(StringCompanionObject_instance), JsonElementSerializer_getInstance()).qf(decoder));
  };
  var JsonObjectSerializer_instance;
  function JsonObjectSerializer_getInstance() {
    if (JsonObjectSerializer_instance == null)
      new JsonObjectSerializer();
    return JsonObjectSerializer_instance;
  }
  function JsonArrayDescriptor() {
    JsonArrayDescriptor_instance = this;
    this.r12_1 = ListSerializer(JsonElementSerializer_getInstance()).of();
    this.s12_1 = 'kotlinx.serialization.json.JsonArray';
  }
  protoOf(JsonArrayDescriptor).tg = function () {
    return this.s12_1;
  };
  protoOf(JsonArrayDescriptor).yg = function (index) {
    return this.r12_1.yg(index);
  };
  protoOf(JsonArrayDescriptor).zg = function (name) {
    return this.r12_1.zg(name);
  };
  protoOf(JsonArrayDescriptor).ah = function (index) {
    return this.r12_1.ah(index);
  };
  protoOf(JsonArrayDescriptor).bh = function (index) {
    return this.r12_1.bh(index);
  };
  protoOf(JsonArrayDescriptor).ch = function (index) {
    return this.r12_1.ch(index);
  };
  protoOf(JsonArrayDescriptor).ug = function () {
    return this.r12_1.ug();
  };
  protoOf(JsonArrayDescriptor).pg = function () {
    return this.r12_1.pg();
  };
  protoOf(JsonArrayDescriptor).vg = function () {
    return this.r12_1.vg();
  };
  protoOf(JsonArrayDescriptor).wg = function () {
    return this.r12_1.wg();
  };
  protoOf(JsonArrayDescriptor).xg = function () {
    return this.r12_1.xg();
  };
  var JsonArrayDescriptor_instance;
  function JsonArrayDescriptor_getInstance() {
    if (JsonArrayDescriptor_instance == null)
      new JsonArrayDescriptor();
    return JsonArrayDescriptor_instance;
  }
  function JsonArraySerializer() {
    JsonArraySerializer_instance = this;
    this.k12_1 = JsonArrayDescriptor_getInstance();
  }
  protoOf(JsonArraySerializer).of = function () {
    return this.k12_1;
  };
  protoOf(JsonArraySerializer).t12 = function (encoder, value) {
    verify(encoder);
    ListSerializer(JsonElementSerializer_getInstance()).pf(encoder, value);
  };
  protoOf(JsonArraySerializer).pf = function (encoder, value) {
    return this.t12(encoder, value instanceof JsonArray ? value : THROW_CCE());
  };
  protoOf(JsonArraySerializer).qf = function (decoder) {
    verify_0(decoder);
    return new JsonArray(ListSerializer(JsonElementSerializer_getInstance()).qf(decoder));
  };
  var JsonArraySerializer_instance;
  function JsonArraySerializer_getInstance() {
    if (JsonArraySerializer_instance == null)
      new JsonArraySerializer();
    return JsonArraySerializer_instance;
  }
  function defer(deferred) {
    return new defer$1(deferred);
  }
  function JsonNullSerializer() {
    JsonNullSerializer_instance = this;
    this.h12_1 = buildSerialDescriptor('kotlinx.serialization.json.JsonNull', ENUM_getInstance(), []);
  }
  protoOf(JsonNullSerializer).of = function () {
    return this.h12_1;
  };
  protoOf(JsonNullSerializer).u12 = function (encoder, value) {
    verify(encoder);
    encoder.lj();
  };
  protoOf(JsonNullSerializer).pf = function (encoder, value) {
    return this.u12(encoder, value instanceof JsonNull ? value : THROW_CCE());
  };
  protoOf(JsonNullSerializer).qf = function (decoder) {
    verify_0(decoder);
    if (decoder.bi()) {
      throw decodingExceptionOf("Expected 'null' literal");
    }
    decoder.ci();
    return JsonNull_getInstance();
  };
  var JsonNullSerializer_instance;
  function JsonNullSerializer_getInstance() {
    if (JsonNullSerializer_instance == null)
      new JsonNullSerializer();
    return JsonNullSerializer_instance;
  }
  function JsonLiteralSerializer() {
    JsonLiteralSerializer_instance = this;
    this.i12_1 = PrimitiveSerialDescriptor('kotlinx.serialization.json.JsonLiteral', STRING_getInstance());
  }
  protoOf(JsonLiteralSerializer).of = function () {
    return this.i12_1;
  };
  protoOf(JsonLiteralSerializer).v12 = function (encoder, value) {
    verify(encoder);
    if (value.c12_1) {
      return encoder.uj(value.e12_1);
    }
    if (!(value.d12_1 == null)) {
      return encoder.wj(value.d12_1).uj(value.e12_1);
    }
    var tmp0_safe_receiver = toLongOrNull(value.e12_1);
    if (tmp0_safe_receiver == null)
      null;
    else {
      // Inline function 'kotlin.let' call
      return encoder.qj(tmp0_safe_receiver);
    }
    var tmp1_safe_receiver = toULongOrNull(value.e12_1);
    var tmp = tmp1_safe_receiver;
    if ((tmp == null ? null : new ULong(tmp)) == null)
      null;
    else {
      var tmp_0 = tmp1_safe_receiver;
      // Inline function 'kotlin.let' call
      var it = (tmp_0 == null ? null : new ULong(tmp_0)).ye_1;
      var tmp_1 = encoder.wj(serializer_0(Companion_getInstance()).of());
      // Inline function 'kotlin.ULong.toLong' call
      var tmp$ret$4 = _ULong___get_data__impl__fggpzb(it);
      tmp_1.qj(tmp$ret$4);
      return Unit_instance;
    }
    var tmp2_safe_receiver = toDoubleOrNull(value.e12_1);
    if (tmp2_safe_receiver == null)
      null;
    else {
      // Inline function 'kotlin.let' call
      return encoder.sj(tmp2_safe_receiver);
    }
    var tmp3_safe_receiver = toBooleanStrictOrNull(value.e12_1);
    if (tmp3_safe_receiver == null)
      null;
    else {
      // Inline function 'kotlin.let' call
      return encoder.mj(tmp3_safe_receiver);
    }
    encoder.uj(value.e12_1);
  };
  protoOf(JsonLiteralSerializer).pf = function (encoder, value) {
    return this.v12(encoder, value instanceof JsonLiteral ? value : THROW_CCE());
  };
  protoOf(JsonLiteralSerializer).qf = function (decoder) {
    var jsonDecoder = asJsonDecoder(decoder);
    var result = jsonDecoder.u11();
    if (!(result instanceof JsonLiteral)) {
      // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
      var shortMessage = 'Unexpected JSON element, expected JsonLiteral, had ' + toString(getKClassFromExpression(result));
      var tmp;
      if (jsonDecoder.t11().iz_1.r11_1) {
        var tmp$ret$3 = toString(result);
        tmp = toString(minify(tmp$ret$3));
      } else {
        tmp = null;
      }
      var inputValue = tmp;
      throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, shortMessage, null, null, inputValue), shortMessage, -1, null, inputValue, null);
    }
    return result;
  };
  var JsonLiteralSerializer_instance;
  function JsonLiteralSerializer_getInstance() {
    if (JsonLiteralSerializer_instance == null)
      new JsonLiteralSerializer();
    return JsonLiteralSerializer_instance;
  }
  function verify(encoder) {
    asJsonEncoder(encoder);
  }
  function asJsonDecoder(_this__u8e3s4) {
    var tmp0_elvis_lhs = isInterface(_this__u8e3s4, JsonDecoder) ? _this__u8e3s4 : null;
    var tmp;
    if (tmp0_elvis_lhs == null) {
      throw IllegalStateException_init_$Create$('This serializer can be used only with Json format.' + ('Expected Decoder to be JsonDecoder, got ' + toString(getKClassFromExpression(_this__u8e3s4))));
    } else {
      tmp = tmp0_elvis_lhs;
    }
    return tmp;
  }
  function verify_0(decoder) {
    asJsonDecoder(decoder);
  }
  function asJsonEncoder(_this__u8e3s4) {
    var tmp0_elvis_lhs = isInterface(_this__u8e3s4, JsonEncoder) ? _this__u8e3s4 : null;
    var tmp;
    if (tmp0_elvis_lhs == null) {
      throw IllegalStateException_init_$Create$('This serializer can be used only with Json format.' + ('Expected Encoder to be JsonEncoder, got ' + toString(getKClassFromExpression(_this__u8e3s4))));
    } else {
      tmp = tmp0_elvis_lhs;
    }
    return tmp;
  }
  function _get_original__l7ku1m($this) {
    var tmp0 = $this.w12_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('original', 1, tmp, defer$o$_get_original_$ref_3cje7k(), null);
    return tmp0.y1();
  }
  function defer$o$_get_original_$ref_3cje7k() {
    return constructCallableReference(function (p0) {
      return _get_original__l7ku1m(p0);
    }, 1, 0, 8);
  }
  function defer$1($deferred) {
    this.w12_1 = lazy($deferred);
  }
  protoOf(defer$1).tg = function () {
    return _get_original__l7ku1m(this).tg();
  };
  protoOf(defer$1).ug = function () {
    return _get_original__l7ku1m(this).ug();
  };
  protoOf(defer$1).wg = function () {
    return _get_original__l7ku1m(this).wg();
  };
  protoOf(defer$1).yg = function (index) {
    return _get_original__l7ku1m(this).yg(index);
  };
  protoOf(defer$1).zg = function (name) {
    return _get_original__l7ku1m(this).zg(name);
  };
  protoOf(defer$1).ah = function (index) {
    return _get_original__l7ku1m(this).ah(index);
  };
  protoOf(defer$1).bh = function (index) {
    return _get_original__l7ku1m(this).bh(index);
  };
  protoOf(defer$1).ch = function (index) {
    return _get_original__l7ku1m(this).ch(index);
  };
  function JsonEncoder() {
  }
  function JsonDecodingException(fullMessage, shortMessage, offset, path, input, hint, cause) {
    cause = cause === VOID ? null : cause;
    JsonException.call(this, fullMessage, cause);
    captureStack(this, JsonDecodingException);
    this.y12_1 = shortMessage;
    this.z12_1 = offset;
    this.a13_1 = path;
    this.b13_1 = input;
    this.c13_1 = hint;
  }
  function JsonEncodingException(shortMessage, classSerialName, hint, cause) {
    classSerialName = classSerialName === VOID ? null : classSerialName;
    hint = hint === VOID ? null : hint;
    cause = cause === VOID ? null : cause;
    JsonException.call(this, formatEncodingException(shortMessage, hint), cause);
    captureStack(this, JsonEncodingException);
    this.f13_1 = shortMessage;
    this.g13_1 = classSerialName;
    this.h13_1 = hint;
  }
  function JsonException(message, cause) {
    SerializationException_init_$Init$(message, cause, this);
    captureStack(this, JsonException);
    this.d13_1 = message;
    delete this.message;
  }
  protoOf(JsonException).d8 = function () {
    return this.d13_1;
  };
  function Composer(writer) {
    this.i13_1 = writer;
    this.j13_1 = true;
  }
  protoOf(Composer).k13 = function () {
    this.j13_1 = true;
  };
  protoOf(Composer).l13 = function () {
    return Unit_instance;
  };
  protoOf(Composer).m13 = function () {
    this.j13_1 = false;
  };
  protoOf(Composer).n13 = function () {
    this.j13_1 = false;
  };
  protoOf(Composer).o13 = function () {
    return Unit_instance;
  };
  protoOf(Composer).p13 = function (v) {
    return this.i13_1.q13(v);
  };
  protoOf(Composer).r13 = function (v) {
    return this.i13_1.s13(v);
  };
  protoOf(Composer).t13 = function (v) {
    return this.i13_1.s13(v.toString());
  };
  protoOf(Composer).u13 = function (v) {
    return this.i13_1.s13(v.toString());
  };
  protoOf(Composer).v13 = function (v) {
    return this.i13_1.w13(fromInt(v));
  };
  protoOf(Composer).x13 = function (v) {
    return this.i13_1.w13(fromInt(v));
  };
  protoOf(Composer).y13 = function (v) {
    return this.i13_1.w13(fromInt(v));
  };
  protoOf(Composer).z13 = function (v) {
    return this.i13_1.w13(v);
  };
  protoOf(Composer).a14 = function (v) {
    return this.i13_1.s13(v.toString());
  };
  protoOf(Composer).b14 = function (value) {
    return this.i13_1.c14(value);
  };
  function Composer_0(sb, json) {
    return json.iz_1.e11_1 ? new ComposerWithPrettyPrint(sb, json) : new Composer(sb);
  }
  function ComposerForUnsignedNumbers(writer, forceQuoting) {
    Composer.call(this, writer);
    this.f14_1 = forceQuoting;
  }
  protoOf(ComposerForUnsignedNumbers).y13 = function (v) {
    if (this.f14_1) {
      // Inline function 'kotlin.toUInt' call
      var tmp$ret$0 = _UInt___init__impl__l7qpdl(v);
      this.b14(UInt__toString_impl_dbgl21(tmp$ret$0));
    } else {
      // Inline function 'kotlin.toUInt' call
      var tmp$ret$1 = _UInt___init__impl__l7qpdl(v);
      this.r13(UInt__toString_impl_dbgl21(tmp$ret$1));
    }
  };
  protoOf(ComposerForUnsignedNumbers).z13 = function (v) {
    if (this.f14_1) {
      // Inline function 'kotlin.toULong' call
      var tmp$ret$0 = _ULong___init__impl__c78o9k(v);
      this.b14(ULong__toString_impl_f9au7k(tmp$ret$0));
    } else {
      // Inline function 'kotlin.toULong' call
      var tmp$ret$1 = _ULong___init__impl__c78o9k(v);
      this.r13(ULong__toString_impl_f9au7k(tmp$ret$1));
    }
  };
  protoOf(ComposerForUnsignedNumbers).v13 = function (v) {
    if (this.f14_1) {
      // Inline function 'kotlin.toUByte' call
      var tmp$ret$0 = _UByte___init__impl__g9hnc4(v);
      this.b14(UByte__toString_impl_v72jg(tmp$ret$0));
    } else {
      // Inline function 'kotlin.toUByte' call
      var tmp$ret$1 = _UByte___init__impl__g9hnc4(v);
      this.r13(UByte__toString_impl_v72jg(tmp$ret$1));
    }
  };
  protoOf(ComposerForUnsignedNumbers).x13 = function (v) {
    if (this.f14_1) {
      // Inline function 'kotlin.toUShort' call
      var tmp$ret$0 = _UShort___init__impl__jigrne(v);
      this.b14(UShort__toString_impl_edaoee(tmp$ret$0));
    } else {
      // Inline function 'kotlin.toUShort' call
      var tmp$ret$1 = _UShort___init__impl__jigrne(v);
      this.r13(UShort__toString_impl_edaoee(tmp$ret$1));
    }
  };
  function ComposerForUnquotedLiterals(writer, forceQuoting) {
    Composer.call(this, writer);
    this.i14_1 = forceQuoting;
  }
  protoOf(ComposerForUnquotedLiterals).b14 = function (value) {
    if (this.i14_1) {
      protoOf(Composer).b14.call(this, value);
    } else {
      protoOf(Composer).r13.call(this, value);
    }
  };
  function ComposerWithPrettyPrint(writer, json) {
    Composer.call(this, writer);
    this.l14_1 = json;
    this.m14_1 = 0;
  }
  protoOf(ComposerWithPrettyPrint).k13 = function () {
    this.j13_1 = true;
    this.m14_1 = this.m14_1 + 1 | 0;
  };
  protoOf(ComposerWithPrettyPrint).l13 = function () {
    this.m14_1 = this.m14_1 - 1 | 0;
  };
  protoOf(ComposerWithPrettyPrint).m13 = function () {
    this.j13_1 = false;
    this.r13('\n');
    // Inline function 'kotlin.repeat' call
    var times = this.m14_1;
    var inductionVariable = 0;
    if (inductionVariable < times)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        this.r13(this.l14_1.iz_1.g11_1);
      }
       while (inductionVariable < times);
  };
  protoOf(ComposerWithPrettyPrint).n13 = function () {
    if (this.j13_1)
      this.j13_1 = false;
    else {
      this.m13();
    }
  };
  protoOf(ComposerWithPrettyPrint).o13 = function () {
    this.p13(_Char___init__impl__6a9atx(32));
  };
  function readIfAbsent($this, descriptor, index) {
    $this.o14_1 = (!descriptor.ch(index) && descriptor.bh(index).pg());
    return $this.o14_1;
  }
  function JsonElementMarker$readIfAbsent$ref(p0) {
    return constructCallableReference(function (p0_0, p1) {
      var tmp0 = p0;
      return readIfAbsent(tmp0, p0_0, p1);
    }, 2, 0, 9, 'readIfAbsent', [p0]);
  }
  function JsonElementMarker(descriptor) {
    var tmp = this;
    tmp.n14_1 = new ElementMarker(descriptor, JsonElementMarker$readIfAbsent$ref(this));
    this.o14_1 = false;
  }
  protoOf(JsonElementMarker).p14 = function (index) {
    this.n14_1.ro(index);
  };
  protoOf(JsonElementMarker).q14 = function () {
    return this.n14_1.so();
  };
  function minify(_this__u8e3s4, offset) {
    offset = offset === VOID ? -1 : offset;
    if (charSequenceLength(_this__u8e3s4) < 200)
      return _this__u8e3s4;
    if (offset === -1) {
      var start = charSequenceLength(_this__u8e3s4) - 60 | 0;
      if (start <= 0)
        return _this__u8e3s4;
      // Inline function 'kotlin.text.substring' call
      var endIndex = charSequenceLength(_this__u8e3s4);
      return '.....' + toString(charSequenceSubSequence(_this__u8e3s4, start, endIndex));
    }
    var start_0 = offset - 30 | 0;
    var end = offset + 30 | 0;
    var prefix = start_0 <= 0 ? '' : '.....';
    var suffix = end >= charSequenceLength(_this__u8e3s4) ? '' : '.....';
    var tmp2 = coerceAtLeast(start_0, 0);
    // Inline function 'kotlin.text.substring' call
    var endIndex_0 = coerceAtMost(end, charSequenceLength(_this__u8e3s4));
    return prefix + toString(charSequenceSubSequence(_this__u8e3s4, tmp2, endIndex_0)) + suffix;
  }
  function access$formatDecodingException$tJsonExceptionsKt(offset, shortMessage, path, hint, input) {
    return formatDecodingException(offset, shortMessage, path, hint, input);
  }
  function invalidTrailingComma(_this__u8e3s4, entity) {
    entity = entity === VOID ? 'object' : entity;
    _this__u8e3s4.r14('Trailing comma before the end of JSON ' + entity, _this__u8e3s4.a10_1 - 1 | 0, "Trailing commas are non-complaint JSON and not allowed by default. Use 'allowTrailingComma = true' in 'Json {}' builder to support them.");
  }
  function throwInvalidFloatingPointDecoded(_this__u8e3s4, result) {
    _this__u8e3s4.s14(nonFiniteFpMessage(result, null), VOID, "It is possible to deserialize them using 'JsonBuilder.allowSpecialFloatingPointValues = true'");
  }
  function InvalidKeyKindException(keyDescriptor) {
    return new JsonEncodingException("Value of type '" + keyDescriptor.tg() + "' can't be used in JSON as a key in the map. " + ("It should have either primitive or enum kind, but its kind is '" + keyDescriptor.ug().toString() + "'"), keyDescriptor.tg(), "Use 'allowStructuredMapKeys = true' in 'Json {}' builder to convert such maps to [key1, value1, key2, value2,...] arrays.");
  }
  function InvalidFloatingPointEncoded(value, key) {
    key = key === VOID ? null : key;
    return new JsonEncodingException(nonFiniteFpMessage(value, key), VOID, "It is possible to deserialize them using 'JsonBuilder.allowSpecialFloatingPointValues = true'");
  }
  function access$nonFiniteFpMessage$tJsonExceptionsKt(value, key) {
    return nonFiniteFpMessage(value, key);
  }
  function decodingExceptionOf(shortMessage, hint) {
    hint = hint === VOID ? null : hint;
    return new JsonDecodingException(formatDecodingException(-1, shortMessage, null, hint, null), shortMessage, -1, null, null, hint);
  }
  function formatEncodingException(shortMessage, hint) {
    var tmp;
    // Inline function 'kotlin.text.isNullOrBlank' call
    if (hint == null || isBlank(hint)) {
      tmp = '';
    } else {
      tmp = '\n' + hint;
    }
    return shortMessage + tmp;
  }
  function decodingExceptionOf_0(_this__u8e3s4, shortMessage, offset, path, hint, input, cause) {
    cause = cause === VOID ? null : cause;
    // Inline function 'kotlinx.serialization.json.internal.ifDebugInput' call
    var tmp;
    if (_this__u8e3s4.zz_1.r11_1) {
      tmp = toString(minify(input, offset));
    } else {
      tmp = null;
    }
    var inputValue = tmp;
    return new JsonDecodingException(formatDecodingException(offset, shortMessage, path, hint, inputValue), shortMessage, offset, path, inputValue, hint, cause);
  }
  function formatDecodingException(offset, shortMessage, path, hint, input) {
    // Inline function 'kotlin.text.buildString' call
    // Inline function 'kotlin.apply' call
    var this_0 = StringBuilder_init_$Create$();
    if (offset >= 0) {
      this_0.o('Unexpected JSON token at offset ' + offset + ': ');
    }
    this_0.o(shortMessage);
    // Inline function 'kotlin.text.isNullOrBlank' call
    if (!(path == null || isBlank(path))) {
      this_0.o(' at path: ');
      this_0.o(path);
    }
    // Inline function 'kotlin.text.isNullOrBlank' call
    if (!(hint == null || isBlank(hint))) {
      this_0.o('\n' + hint);
    }
    if (!(input == null)) {
      this_0.o('\nJSON input: ');
      this_0.o(input);
    }
    return this_0.toString();
  }
  function nonFiniteFpMessage(value, key) {
    return 'Unexpected special floating-point value ' + toString(value) + (!(key == null) ? ' with key ' + key + '. ' : '. ') + 'By default, ' + 'non-finite floating point values are prohibited because they do not conform JSON specification.';
  }
  function get_JsonDeserializationNamesKey() {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    return JsonDeserializationNamesKey;
  }
  var JsonDeserializationNamesKey;
  function get_JsonSerializationNamesKey() {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    return JsonSerializationNamesKey;
  }
  var JsonSerializationNamesKey;
  function ignoreUnknownKeys(_this__u8e3s4, json) {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    var tmp;
    if (json.iz_1.b11_1) {
      tmp = true;
    } else {
      var tmp0 = _this__u8e3s4.xg();
      var tmp$ret$0;
      $l$block_0: {
        // Inline function 'kotlin.collections.any' call
        var tmp_0;
        if (isInterface(tmp0, Collection)) {
          tmp_0 = tmp0.p();
        } else {
          tmp_0 = false;
        }
        if (tmp_0) {
          tmp$ret$0 = false;
          break $l$block_0;
        }
        var _iterator__ex2g4s = tmp0.r();
        while (_iterator__ex2g4s.s()) {
          var element = _iterator__ex2g4s.t();
          if (element instanceof JsonIgnoreUnknownKeys) {
            tmp$ret$0 = true;
            break $l$block_0;
          }
        }
        tmp$ret$0 = false;
      }
      tmp = tmp$ret$0;
    }
    return tmp;
  }
  function getJsonNameIndex(_this__u8e3s4, json, name) {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    if (decodeCaseInsensitive(json, _this__u8e3s4)) {
      // Inline function 'kotlin.text.lowercase' call
      // Inline function 'kotlin.js.asDynamic' call
      var tmp$ret$0 = name.toLowerCase();
      return getJsonNameIndexSlowPath(_this__u8e3s4, json, tmp$ret$0);
    }
    var strategy = namingStrategy(_this__u8e3s4, json);
    if (!(strategy == null))
      return getJsonNameIndexSlowPath(_this__u8e3s4, json, name);
    var index = _this__u8e3s4.zg(name);
    if (!(index === -3))
      return index;
    if (!json.iz_1.l11_1)
      return index;
    return getJsonNameIndexSlowPath(_this__u8e3s4, json, name);
  }
  function getJsonNameIndexOrThrow(_this__u8e3s4, json, name, suffix) {
    suffix = suffix === VOID ? '' : suffix;
    _init_properties_JsonNamesMap_kt__cbbp0k();
    var index = getJsonNameIndex(_this__u8e3s4, json, name);
    if (index === -3)
      throw SerializationException_init_$Create$(_this__u8e3s4.tg() + " does not contain element with name '" + name + "'" + suffix);
    return index;
  }
  function getJsonElementName(_this__u8e3s4, json, index) {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    var strategy = namingStrategy(_this__u8e3s4, json);
    return strategy == null ? _this__u8e3s4.yg(index) : serializationNamesIndices(_this__u8e3s4, json, strategy)[index];
  }
  function getJsonEncodedNames(_this__u8e3s4, json) {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    var strategy = namingStrategy(_this__u8e3s4, json);
    return strategy == null ? jsonCachedSerialNames(_this__u8e3s4) : toSet(serializationNamesIndices(_this__u8e3s4, json, strategy));
  }
  function decodeCaseInsensitive(_this__u8e3s4, descriptor) {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    return _this__u8e3s4.iz_1.n11_1 && equals(descriptor.ug(), ENUM_getInstance());
  }
  function getJsonNameIndexSlowPath(_this__u8e3s4, json, name) {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    var tmp0_elvis_lhs = deserializationNamesMap(json, _this__u8e3s4).b2(name);
    return tmp0_elvis_lhs == null ? -3 : tmp0_elvis_lhs;
  }
  function namingStrategy(_this__u8e3s4, json) {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    return equals(_this__u8e3s4.ug(), CLASS_getInstance()) ? json.iz_1.m11_1 : null;
  }
  function serializationNamesIndices(_this__u8e3s4, json, strategy) {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    var tmp = get_schemaCache(json);
    var tmp_0 = get_JsonSerializationNamesKey();
    return tmp.u14(_this__u8e3s4, tmp_0, serializationNamesIndices$lambda(_this__u8e3s4, strategy));
  }
  function deserializationNamesMap(_this__u8e3s4, descriptor) {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    var tmp = get_schemaCache(_this__u8e3s4);
    var tmp_0 = get_JsonDeserializationNamesKey();
    return tmp.u14(descriptor, tmp_0, deserializationNamesMap$lambda(descriptor, _this__u8e3s4));
  }
  function buildDeserializationNamesMap(_this__u8e3s4, json) {
    _init_properties_JsonNamesMap_kt__cbbp0k();
    // Inline function 'kotlin.collections.mutableMapOf' call
    var builder = LinkedHashMap_init_$Create$();
    var useLowercaseEnums = decodeCaseInsensitive(json, _this__u8e3s4);
    var strategyForClasses = namingStrategy(_this__u8e3s4, json);
    var inductionVariable = 0;
    var last = _this__u8e3s4.wg();
    if (inductionVariable < last)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        // Inline function 'kotlin.collections.filterIsInstance' call
        var tmp0 = _this__u8e3s4.ah(i);
        // Inline function 'kotlin.collections.filterIsInstanceTo' call
        var destination = ArrayList_init_$Create$();
        var _iterator__ex2g4s = tmp0.r();
        while (_iterator__ex2g4s.s()) {
          var element = _iterator__ex2g4s.t();
          if (element instanceof JsonNames) {
            destination.w(element);
          }
        }
        var tmp0_safe_receiver = singleOrNull(destination);
        var tmp1_safe_receiver = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.v14_1;
        if (tmp1_safe_receiver == null)
          null;
        else {
          // Inline function 'kotlin.collections.forEach' call
          var inductionVariable_0 = 0;
          var last_0 = tmp1_safe_receiver.length;
          while (inductionVariable_0 < last_0) {
            var element_0 = tmp1_safe_receiver[inductionVariable_0];
            inductionVariable_0 = inductionVariable_0 + 1 | 0;
            var tmp;
            if (useLowercaseEnums) {
              // Inline function 'kotlin.text.lowercase' call
              // Inline function 'kotlin.js.asDynamic' call
              tmp = element_0.toLowerCase();
            } else {
              tmp = element_0;
            }
            buildDeserializationNamesMap$putOrThrow(builder, _this__u8e3s4, tmp, i);
          }
        }
        var tmp_0;
        if (useLowercaseEnums) {
          // Inline function 'kotlin.text.lowercase' call
          // Inline function 'kotlin.js.asDynamic' call
          tmp_0 = _this__u8e3s4.yg(i).toLowerCase();
        } else if (!(strategyForClasses == null)) {
          tmp_0 = strategyForClasses.w14(_this__u8e3s4, i, _this__u8e3s4.yg(i));
        } else {
          tmp_0 = null;
        }
        var nameToPut = tmp_0;
        if (nameToPut == null)
          null;
        else {
          // Inline function 'kotlin.let' call
          buildDeserializationNamesMap$putOrThrow(builder, _this__u8e3s4, nameToPut, i);
        }
      }
       while (inductionVariable < last);
    // Inline function 'kotlin.collections.ifEmpty' call
    var tmp_1;
    if (builder.p()) {
      tmp_1 = emptyMap();
    } else {
      tmp_1 = builder;
    }
    return tmp_1;
  }
  function serializationNamesIndices$lambda($this_serializationNamesIndices, $strategy) {
    return function () {
      // Inline function 'kotlin.collections.mutableSetOf' call
      var trackingSet = LinkedHashSet_init_$Create$();
      var tmp = 0;
      var tmp_0 = $this_serializationNamesIndices.wg();
      // Inline function 'kotlin.arrayOfNulls' call
      var tmp_1 = Array(tmp_0);
      while (tmp < tmp_0) {
        var tmp_2 = tmp;
        var baseName = $this_serializationNamesIndices.yg(tmp_2);
        var name = $strategy.w14($this_serializationNamesIndices, tmp_2, baseName);
        if (!trackingSet.w(name))
          throw new JsonEncodingException("The transformed name '" + name + "' for property " + baseName + ' already exists ' + ('in ' + toString($this_serializationNamesIndices)), $this_serializationNamesIndices.tg());
        tmp_1[tmp_2] = name;
        tmp = tmp + 1 | 0;
      }
      return tmp_1;
    };
  }
  function deserializationNamesMap$lambda($descriptor, $this_deserializationNamesMap) {
    return function () {
      return buildDeserializationNamesMap($descriptor, $this_deserializationNamesMap);
    };
  }
  function buildDeserializationNamesMap$putOrThrow(_this__u8e3s4, $this_buildDeserializationNamesMap, name, index) {
    var entity = equals($this_buildDeserializationNamesMap.ug(), ENUM_getInstance()) ? 'enum value' : 'property';
    // Inline function 'kotlin.collections.contains' call
    // Inline function 'kotlin.collections.containsKey' call
    if ((isInterface(_this__u8e3s4, KtMap) ? _this__u8e3s4 : THROW_CCE()).z1(name)) {
      throw decodingExceptionOf("The suggested name '" + name + "' for " + entity + ' ' + $this_buildDeserializationNamesMap.yg(index) + ' is already one of the names for ' + entity + ' ' + ($this_buildDeserializationNamesMap.yg(getValue(_this__u8e3s4, name)) + ' in ' + toString($this_buildDeserializationNamesMap)));
    }
    // Inline function 'kotlin.collections.set' call
    _this__u8e3s4.h2(name, index);
  }
  var properties_initialized_JsonNamesMap_kt_ljpf42;
  function _init_properties_JsonNamesMap_kt__cbbp0k() {
    if (!properties_initialized_JsonNamesMap_kt_ljpf42) {
      properties_initialized_JsonNamesMap_kt_ljpf42 = true;
      JsonDeserializationNamesKey = new Key();
      JsonSerializationNamesKey = new Key();
    }
  }
  function Tombstone() {
  }
  var Tombstone_instance;
  function Tombstone_getInstance() {
    return Tombstone_instance;
  }
  function RedactedKey() {
  }
  var RedactedKey_instance;
  function RedactedKey_getInstance() {
    return RedactedKey_instance;
  }
  function resize($this) {
    var newSize = imul($this.b15_1, 2);
    $this.z14_1 = copyOf($this.z14_1, newSize);
    var tmp = 0;
    var tmp_0 = new Int32Array(newSize);
    while (tmp < newSize) {
      tmp_0[tmp] = -1;
      tmp = tmp + 1 | 0;
    }
    var newIndices = tmp_0;
    // Inline function 'kotlin.collections.copyInto' call
    var this_0 = $this.a15_1;
    var endIndex = this_0.length;
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    var tmp_1 = this_0;
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    arrayCopy(tmp_1, newIndices, 0, 0, endIndex);
    $this.a15_1 = newIndices;
  }
  function JsonPath(configuration) {
    this.x14_1 = configuration.s11_1;
    this.y14_1 = configuration.r11_1;
    var tmp = this;
    // Inline function 'kotlin.arrayOfNulls' call
    tmp.z14_1 = Array(8);
    var tmp_0 = this;
    var tmp_1 = 0;
    var tmp_2 = new Int32Array(8);
    while (tmp_1 < 8) {
      tmp_2[tmp_1] = -1;
      tmp_1 = tmp_1 + 1 | 0;
    }
    tmp_0.a15_1 = tmp_2;
    this.b15_1 = -1;
  }
  protoOf(JsonPath).c15 = function (sd) {
    this.b15_1 = this.b15_1 + 1 | 0;
    var depth = this.b15_1;
    if (depth >= this.x14_1) {
      jsonTooNested(this.x14_1);
    }
    if (depth === this.z14_1.length) {
      resize(this);
    }
    this.z14_1[depth] = sd;
  };
  protoOf(JsonPath).d15 = function (index) {
    this.a15_1[this.b15_1] = index;
  };
  protoOf(JsonPath).e15 = function (key) {
    var tmp;
    if (!(this.a15_1[this.b15_1] === -2)) {
      this.b15_1 = this.b15_1 + 1 | 0;
      tmp = this.b15_1 === this.z14_1.length;
    } else {
      tmp = false;
    }
    if (tmp) {
      resize(this);
    }
    this.z14_1[this.b15_1] = this.y14_1 ? key : RedactedKey_instance;
    this.a15_1[this.b15_1] = -2;
  };
  protoOf(JsonPath).f15 = function () {
    if (this.a15_1[this.b15_1] === -2) {
      this.z14_1[this.b15_1] = Tombstone_instance;
    }
  };
  protoOf(JsonPath).g15 = function () {
    var depth = this.b15_1;
    if (this.a15_1[depth] === -2) {
      this.a15_1[depth] = -1;
      this.b15_1 = this.b15_1 - 1 | 0;
    }
    if (!(this.b15_1 === -1)) {
      this.b15_1 = this.b15_1 - 1 | 0;
    }
  };
  protoOf(JsonPath).h15 = function () {
    // Inline function 'kotlin.text.buildString' call
    // Inline function 'kotlin.apply' call
    var this_0 = StringBuilder_init_$Create$();
    this_0.o('$');
    // Inline function 'kotlin.repeat' call
    var times = this.b15_1 + 1 | 0;
    var inductionVariable = 0;
    if (inductionVariable < times)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        var element = this.z14_1[index];
        if (!(element == null) ? isInterface(element, SerialDescriptor) : false) {
          if (equals(element.ug(), LIST_getInstance())) {
            if (!(this.a15_1[index] === -1)) {
              this_0.o('[');
              this_0.l9(this.a15_1[index]);
              this_0.o(']');
            }
          } else {
            var idx = this.a15_1[index];
            if (idx >= 0) {
              this_0.o('.');
              this_0.o(element.yg(idx));
            }
          }
        } else {
          if (element === RedactedKey_instance) {
            this_0.o('[<debug info disabled>]');
          } else {
            if (!(element === Tombstone_instance)) {
              this_0.o('[');
              this_0.o("'");
              this_0.u(element);
              this_0.o("'");
              this_0.o(']');
            }
          }
        }
      }
       while (inductionVariable < times);
    return this_0.toString();
  };
  protoOf(JsonPath).toString = function () {
    return this.h15();
  };
  function classDiscriminator(_this__u8e3s4, json) {
    var _iterator__ex2g4s = _this__u8e3s4.xg().r();
    while (_iterator__ex2g4s.s()) {
      var annotation = _iterator__ex2g4s.t();
      if (annotation instanceof JsonClassDiscriminator)
        return annotation.i15_1;
    }
    return json.iz_1.j11_1;
  }
  function subtypeNotRegisteredMessageJson(subClassName, baseClass) {
    var scope = "in the polymorphic scope of '" + baseClass.m() + "'";
    var tmp;
    if (subClassName == null) {
      tmp = to('Class discriminator was missing and no default serializers were registered ' + scope, null);
    } else {
      tmp = to("Serializer for subclass '" + subClassName + "' is not found " + scope, "Check if class with serial name '" + subClassName + "' exists and serializer is registered in a corresponding SerializersModule.\n" + ("To be registered automatically, class '" + subClassName + "' has to be '@Serializable', and the base class '" + baseClass.m() + "' has to be sealed and '@Serializable'."));
    }
    return tmp;
  }
  function access$checkEncodingConflicts$tJsonPolymorphicHelpersKt($receiver, serializer, actualSerializer, classDiscriminator) {
    return checkEncodingConflicts($receiver, serializer, actualSerializer, classDiscriminator);
  }
  function checkKind(kind) {
    if (kind instanceof ENUM) {
      // Inline function 'kotlin.error' call
      var message = "Enums cannot be serialized polymorphically with 'type' parameter. You can use 'JsonBuilder.useArrayPolymorphism' instead";
      throw IllegalStateException_init_$Create$(toString(message));
    }
    if (kind instanceof PrimitiveKind) {
      // Inline function 'kotlin.error' call
      var message_0 = "Primitives cannot be serialized polymorphically with 'type' parameter. You can use 'JsonBuilder.useArrayPolymorphism' instead";
      throw IllegalStateException_init_$Create$(toString(message_0));
    }
    if (kind instanceof PolymorphicKind) {
      // Inline function 'kotlin.error' call
      var message_1 = 'Actual serializer for polymorphic cannot be polymorphic itself';
      throw IllegalStateException_init_$Create$(toString(message_1));
    }
  }
  function checkEncodingConflicts(_this__u8e3s4, serializer, actualSerializer, classDiscriminator) {
    if (getJsonEncodedNames(actualSerializer.of(), _this__u8e3s4).v1(classDiscriminator)) {
      var baseName = serializer.of().tg();
      var actualName = actualSerializer.of().tg();
      var text = _this__u8e3s4.iz_1.q11_1.equals(ClassDiscriminatorMode_ALL_JSON_OBJECTS_getInstance()) && baseName === actualName ? 'in ALL_JSON_OBJECTS class discriminator mode' : "as base class '" + baseName + "'";
      var tmp0_shortMessage = "Class '" + actualName + "' cannot be serialized " + text + ' because' + (" it has property name that conflicts with JSON class discriminator '" + classDiscriminator + "'.");
      var tmp1_hint = 'You can either change class discriminator in JsonConfiguration, or rename property with @SerialName annotation.';
      throw new JsonEncodingException(tmp0_shortMessage, actualName, tmp1_hint);
    }
  }
  function checkKind_0($this, descriptor, actualClass) {
    var kind = descriptor.ug();
    var tmp;
    if (kind instanceof PolymorphicKind) {
      tmp = true;
    } else {
      tmp = equals(kind, CONTEXTUAL_getInstance());
    }
    if (tmp) {
      throw IllegalArgumentException_init_$Create$('Serializer for ' + actualClass.m() + " can't be registered as a subclass for polymorphic serialization " + ('because its kind ' + kind.toString() + ' is not concrete. To work with multiple hierarchies, register it as a base class.'));
    }
    if ($this.j15_1)
      return Unit_instance;
    if (!$this.k15_1)
      return Unit_instance;
    var tmp_0;
    var tmp_1;
    if (equals(kind, LIST_getInstance()) || equals(kind, MAP_getInstance())) {
      tmp_1 = true;
    } else {
      tmp_1 = kind instanceof PrimitiveKind;
    }
    if (tmp_1) {
      tmp_0 = true;
    } else {
      tmp_0 = kind instanceof ENUM;
    }
    if (tmp_0) {
      throw IllegalArgumentException_init_$Create$('Serializer for ' + actualClass.m() + ' of kind ' + kind.toString() + ' cannot be serialized polymorphically with class discriminator.');
    }
  }
  function JsonSerializersModuleValidator(configuration) {
    this.j15_1 = configuration.i11_1;
    this.k15_1 = !configuration.q11_1.equals(ClassDiscriminatorMode_NONE_getInstance());
  }
  protoOf(JsonSerializersModuleValidator).zy = function (kClass, provider) {
  };
  protoOf(JsonSerializersModuleValidator).cz = function (baseClass, actualClass, actualSerializer) {
    var descriptor = actualSerializer.of();
    checkKind_0(this, descriptor, actualClass);
  };
  protoOf(JsonSerializersModuleValidator).dz = function (baseClass, defaultSerializerProvider) {
  };
  protoOf(JsonSerializersModuleValidator).ez = function (baseClass, defaultDeserializerProvider) {
  };
  function encodeByWriter(json, writer, serializer, value) {
    var tmp = LexerMode_OBJ_getInstance();
    // Inline function 'kotlin.arrayOfNulls' call
    var size = get_entries().x();
    var tmp$ret$0 = Array(size);
    var encoder = StreamingJsonEncoder_init_$Create$(writer, json, tmp, tmp$ret$0);
    encoder.ik(serializer, value);
  }
  function readObject($this) {
    // Inline function 'kotlinx.serialization.json.internal.JsonTreeReader.readObjectImpl' call
    var lastToken = $this.u15_1.z15(6);
    if ($this.u15_1.a16() === 4) {
      $this.u15_1.s14('Unexpected leading comma');
    }
    // Inline function 'kotlin.collections.linkedMapOf' call
    var result = LinkedHashMap_init_$Create$();
    $l$loop: while ($this.u15_1.b16()) {
      var key = $this.v15_1 ? $this.u15_1.d16() : $this.u15_1.c16();
      $this.u15_1.z15(5);
      var element = $this.e16();
      // Inline function 'kotlin.collections.set' call
      result.h2(key, element);
      lastToken = $this.u15_1.f16();
      var tmp0_subject = lastToken;
      if (tmp0_subject !== 4)
        if (tmp0_subject === 7)
          break $l$loop;
        else {
          $this.u15_1.s14('Expected end of the object or comma');
        }
    }
    if (lastToken === 6) {
      $this.u15_1.z15(7);
    } else if (lastToken === 4) {
      if (!$this.w15_1) {
        invalidTrailingComma($this.u15_1);
      }
      $this.u15_1.z15(7);
    }
    return new JsonObject(result);
  }
  function readObject_0($this, $receiver, $completion) {
    var tmp = new $readObjectCOROUTINE$($this, $receiver, $completion);
    tmp.i7_1 = Unit_instance;
    tmp.j7_1 = null;
    return tmp.o7();
  }
  function readArray($this) {
    var lastToken = $this.u15_1.f16();
    if ($this.u15_1.a16() === 4) {
      $this.u15_1.s14('Unexpected leading comma');
    }
    // Inline function 'kotlin.collections.arrayListOf' call
    var result = ArrayList_init_$Create$();
    while ($this.u15_1.b16()) {
      var element = $this.e16();
      result.w(element);
      lastToken = $this.u15_1.f16();
      if (!(lastToken === 4)) {
        var tmp0 = $this.u15_1;
        // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.require' call
        var condition = lastToken === 9;
        var position = tmp0.a10_1;
        if (!condition) {
          var tmp$ret$2 = 'Expected end of the array or comma';
          tmp0.s14(tmp$ret$2, position);
        }
      }
    }
    if (lastToken === 8) {
      $this.u15_1.z15(9);
    } else if (lastToken === 4) {
      if (!$this.w15_1) {
        invalidTrailingComma($this.u15_1, 'array');
      }
      $this.u15_1.z15(9);
    }
    return new JsonArray(result);
  }
  function readValue($this, isString) {
    var tmp;
    if ($this.v15_1 || !isString) {
      tmp = $this.u15_1.d16();
    } else {
      tmp = $this.u15_1.c16();
    }
    var string = tmp;
    if (!isString && string === 'null')
      return JsonNull_getInstance();
    return new JsonLiteral(string, isString);
  }
  function readDeepRecursive($this) {
    return invoke(new DeepRecursiveFunction(JsonTreeReader$readDeepRecursive$slambda_0($this, null)), Unit_instance);
  }
  function JsonTreeReader$readDeepRecursive$slambda(this$0, resultContinuation) {
    this.c17_1 = this$0;
    CoroutineImpl.call(this, resultContinuation);
  }
  protoOf(JsonTreeReader$readDeepRecursive$slambda).h17 = function ($this$DeepRecursiveFunction, it, $completion) {
    var tmp = this.i17($this$DeepRecursiveFunction, it, $completion);
    tmp.i7_1 = Unit_instance;
    tmp.j7_1 = null;
    return tmp.o7();
  };
  protoOf(JsonTreeReader$readDeepRecursive$slambda).u7 = function (p1, p2, $completion) {
    var tmp = p1 instanceof DeepRecursiveScope ? p1 : THROW_CCE();
    return this.h17(tmp, p2 instanceof Unit ? p2 : THROW_CCE(), $completion);
  };
  protoOf(JsonTreeReader$readDeepRecursive$slambda).o7 = function () {
    var suspendResult = this.i7_1;
    $sm: do
      try {
        var tmp = this.g7_1;
        switch (tmp) {
          case 0:
            this.h7_1 = 3;
            var tmp0_subject = this.c17_1.u15_1.a16();
            if (tmp0_subject === 1) {
              this.f17_1 = readValue(this.c17_1, true);
              this.g7_1 = 2;
              continue $sm;
            } else {
              if (tmp0_subject === 0) {
                this.f17_1 = readValue(this.c17_1, false);
                this.g7_1 = 2;
                continue $sm;
              } else {
                if (tmp0_subject === 6) {
                  this.g17_1 = this.c17_1;
                  this.g17_1.y15_1 = this.g17_1.y15_1 + 1 | 0;
                  if (this.g17_1.y15_1 >= this.g17_1.x15_1) {
                    jsonTooNested(this.g17_1.x15_1);
                  }
                  this.g7_1 = 1;
                  suspendResult = readObject_0(this.c17_1, this.d17_1, this);
                  if (suspendResult === get_COROUTINE_SUSPENDED()) {
                    return suspendResult;
                  }
                  continue $sm;
                } else {
                  if (tmp0_subject === 8) {
                    var tmp_0 = this;
                    var this_0 = this.c17_1;
                    this_0.y15_1 = this_0.y15_1 + 1 | 0;
                    if (this_0.y15_1 >= this_0.x15_1) {
                      jsonTooNested(this_0.x15_1);
                    }
                    var result = readArray(this.c17_1);
                    this_0.y15_1 = this_0.y15_1 - 1 | 0;
                    tmp_0.f17_1 = result;
                    this.g7_1 = 2;
                    continue $sm;
                  } else {
                    var tmp_1 = this;
                    this.c17_1.u15_1.s14("Can't begin reading element, unexpected token");
                  }
                }
              }
            }

            break;
          case 1:
            var result_0 = suspendResult;
            this.g17_1.y15_1 = this.g17_1.y15_1 - 1 | 0;
            this.f17_1 = result_0;
            this.g7_1 = 2;
            continue $sm;
          case 2:
            return this.f17_1;
          case 3:
            throw this.j7_1;
        }
      } catch ($p) {
        var e = $p;
        if (this.h7_1 === 3) {
          throw e;
        } else {
          this.g7_1 = this.h7_1;
          this.j7_1 = e;
        }
      }
     while (true);
  };
  protoOf(JsonTreeReader$readDeepRecursive$slambda).i17 = function ($this$DeepRecursiveFunction, it, completion) {
    var i = new JsonTreeReader$readDeepRecursive$slambda(this.c17_1, completion);
    i.d17_1 = $this$DeepRecursiveFunction;
    i.e17_1 = it;
    return i;
  };
  function JsonTreeReader$readDeepRecursive$slambda_0(this$0, resultContinuation) {
    var i = new JsonTreeReader$readDeepRecursive$slambda(this$0, resultContinuation);
    return constructCallableReference(function ($this$DeepRecursiveFunction, it, $completion) {
      return i.h17($this$DeepRecursiveFunction, it, $completion);
    }, 2);
  }
  function $readObjectCOROUTINE$(_this__u8e3s4, _this__u8e3s4_0, resultContinuation) {
    CoroutineImpl.call(this, resultContinuation);
    this.o16_1 = _this__u8e3s4;
    this.p16_1 = _this__u8e3s4_0;
  }
  protoOf($readObjectCOROUTINE$).o7 = function () {
    var suspendResult = this.i7_1;
    $sm: do
      try {
        var tmp = this.g7_1;
        switch (tmp) {
          case 0:
            this.h7_1 = 5;
            this.r16_1 = this.o16_1;
            this.s16_1 = this.r16_1.u15_1.z15(6);
            if (this.r16_1.u15_1.a16() === 4) {
              this.r16_1.u15_1.s14('Unexpected leading comma');
            }

            var tmp_0 = this;
            tmp_0.q16_1 = LinkedHashMap_init_$Create$();
            this.g7_1 = 1;
            continue $sm;
          case 1:
            if (!this.r16_1.u15_1.b16()) {
              this.g7_1 = 4;
              continue $sm;
            }

            this.t16_1 = this.r16_1.v15_1 ? this.r16_1.u15_1.d16() : this.r16_1.u15_1.c16();
            this.r16_1.u15_1.z15(5);
            this.g7_1 = 2;
            suspendResult = this.p16_1.fd(Unit_instance, this);
            if (suspendResult === get_COROUTINE_SUSPENDED()) {
              return suspendResult;
            }

            continue $sm;
          case 2:
            var element = suspendResult;
            var tmp0 = this.q16_1;
            var key = this.t16_1;
            tmp0.h2(key, element);
            this.s16_1 = this.r16_1.u15_1.f16();
            var tmp0_subject = this.s16_1;
            if (tmp0_subject === 4) {
              this.g7_1 = 3;
              continue $sm;
            } else {
              if (tmp0_subject === 7) {
                this.g7_1 = 4;
                continue $sm;
              } else {
                this.r16_1.u15_1.s14('Expected end of the object or comma');
              }
            }

            break;
          case 3:
            this.g7_1 = 1;
            continue $sm;
          case 4:
            if (this.s16_1 === 6) {
              this.r16_1.u15_1.z15(7);
            } else if (this.s16_1 === 4) {
              if (!this.r16_1.w15_1) {
                invalidTrailingComma(this.r16_1.u15_1);
              }
              this.r16_1.u15_1.z15(7);
            }

            return new JsonObject(this.q16_1);
          case 5:
            throw this.j7_1;
        }
      } catch ($p) {
        var e = $p;
        if (this.h7_1 === 5) {
          throw e;
        } else {
          this.g7_1 = this.h7_1;
          this.j7_1 = e;
        }
      }
     while (true);
  };
  function JsonTreeReader(configuration, lexer) {
    this.u15_1 = lexer;
    this.v15_1 = configuration.c11_1;
    this.w15_1 = configuration.o11_1;
    this.x15_1 = configuration.s11_1;
    this.y15_1 = this.u15_1.b10_1.b15_1;
  }
  protoOf(JsonTreeReader).e16 = function () {
    var token = this.u15_1.a16();
    var tmp;
    if (token === 1) {
      tmp = readValue(this, true);
    } else if (token === 0) {
      tmp = readValue(this, false);
    } else if (token === 6) {
      this.y15_1 = this.y15_1 + 1 | 0;
      if (this.y15_1 >= this.x15_1) {
        jsonTooNested(this.x15_1);
      }
      var result;
      if (this.y15_1 === 200) {
        this.y15_1 = this.y15_1 - 1 | 0;
        result = readDeepRecursive(this);
      } else {
        result = readObject(this);
        this.y15_1 = this.y15_1 - 1 | 0;
      }
      tmp = result;
    } else if (token === 8) {
      // Inline function 'kotlinx.serialization.json.internal.JsonTreeReader.withDepthControl' call
      this.y15_1 = this.y15_1 + 1 | 0;
      if (this.y15_1 >= this.x15_1) {
        jsonTooNested(this.x15_1);
      }
      var result_0 = readArray(this);
      this.y15_1 = this.y15_1 - 1 | 0;
      tmp = result_0;
    } else {
      this.u15_1.s14('Cannot read Json element because of unexpected ' + tokenDescription(token));
    }
    return tmp;
  };
  function jsonTooNested(maxStackDepth) {
    throw decodingExceptionOf('Json input is too nested and may be impossible to parse without stack overflow.', 'Current nesting limit is ' + maxStackDepth + ". To adjust it, use 'maxNestingDepth' in 'Json {}' builder.");
  }
  var static_init_called_0;
  function static_init_0() {
    if (static_init_called_0)
      return Unit_instance;
    static_init_called_0 = true;
    LexerMode_OBJ_instance = new LexerMode('OBJ', 0, _Char___init__impl__6a9atx(123), _Char___init__impl__6a9atx(125));
    LexerMode_LIST_instance = new LexerMode('LIST', 1, _Char___init__impl__6a9atx(91), _Char___init__impl__6a9atx(93));
    LexerMode_MAP_instance = new LexerMode('MAP', 2, _Char___init__impl__6a9atx(123), _Char___init__impl__6a9atx(125));
    LexerMode_POLY_OBJ_instance = new LexerMode('POLY_OBJ', 3, _Char___init__impl__6a9atx(91), _Char___init__impl__6a9atx(93));
  }
  var LexerMode_OBJ_instance;
  var LexerMode_LIST_instance;
  var LexerMode_MAP_instance;
  var LexerMode_POLY_OBJ_instance;
  function values() {
    static_init_0();
    return [LexerMode_OBJ_getInstance(), LexerMode_LIST_getInstance(), LexerMode_MAP_getInstance(), LexerMode_POLY_OBJ_getInstance()];
  }
  function get_entries() {
    static_init_0();
    if ($ENTRIES == null)
      $ENTRIES = enumEntries(values());
    return $ENTRIES;
  }
  var $ENTRIES;
  function LexerMode(name, ordinal, begin, end) {
    Enum.call(this, name, ordinal);
    this.l17_1 = begin;
    this.m17_1 = end;
  }
  function modeFor(_this__u8e3s4, desc) {
    var tmp0_subject = desc.ug();
    var tmp;
    if (tmp0_subject instanceof PolymorphicKind) {
      tmp = LexerMode_POLY_OBJ_getInstance();
    } else {
      if (equals(tmp0_subject, LIST_getInstance())) {
        tmp = LexerMode_LIST_getInstance();
      } else {
        if (equals(tmp0_subject, MAP_getInstance())) {
          // Inline function 'kotlinx.serialization.json.internal.selectMapMode' call
          var keyDescriptor = carrierDescriptor(desc.bh(0), _this__u8e3s4.fj());
          var keyKind = keyDescriptor.ug();
          var tmp_0;
          var tmp_1;
          if (keyKind instanceof PrimitiveKind) {
            tmp_1 = true;
          } else {
            tmp_1 = equals(keyKind, ENUM_getInstance());
          }
          if (tmp_1) {
            tmp_0 = LexerMode_MAP_getInstance();
          } else {
            if (_this__u8e3s4.iz_1.d11_1) {
              tmp_0 = LexerMode_LIST_getInstance();
            } else {
              throw InvalidKeyKindException(keyDescriptor);
            }
          }
          tmp = tmp_0;
        } else {
          tmp = LexerMode_OBJ_getInstance();
        }
      }
    }
    return tmp;
  }
  function carrierDescriptor(_this__u8e3s4, module_0) {
    var tmp;
    if (equals(_this__u8e3s4.ug(), CONTEXTUAL_getInstance())) {
      var tmp0_safe_receiver = getContextualDescriptor(module_0, _this__u8e3s4);
      var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : carrierDescriptor(tmp0_safe_receiver, module_0);
      tmp = tmp1_elvis_lhs == null ? _this__u8e3s4 : tmp1_elvis_lhs;
    } else if (_this__u8e3s4.vg()) {
      tmp = carrierDescriptor(_this__u8e3s4.bh(0), module_0);
    } else {
      tmp = _this__u8e3s4;
    }
    return tmp;
  }
  function LexerMode_OBJ_getInstance() {
    static_init_0();
    return LexerMode_OBJ_instance;
  }
  function LexerMode_LIST_getInstance() {
    static_init_0();
    return LexerMode_LIST_instance;
  }
  function LexerMode_MAP_getInstance() {
    static_init_0();
    return LexerMode_MAP_instance;
  }
  function LexerMode_POLY_OBJ_getInstance() {
    static_init_0();
    return LexerMode_POLY_OBJ_instance;
  }
  function PolymorphicJsonDecoder() {
  }
  function Key() {
  }
  function DescriptorSchemaCache() {
    this.t14_1 = createMapForCache(16);
  }
  protoOf(DescriptorSchemaCache).o17 = function (descriptor, key, value) {
    // Inline function 'kotlin.collections.getOrPut' call
    var this_0 = this.t14_1;
    var value_0 = this_0.b2(descriptor);
    var tmp;
    if (value_0 == null) {
      var answer = createMapForCache(2);
      this_0.h2(descriptor, answer);
      tmp = answer;
    } else {
      tmp = value_0;
    }
    var tmp0 = tmp;
    var tmp2 = key instanceof Key ? key : THROW_CCE();
    // Inline function 'kotlin.collections.set' call
    var value_1 = !(value == null) ? value : THROW_CCE();
    tmp0.h2(tmp2, value_1);
  };
  protoOf(DescriptorSchemaCache).u14 = function (descriptor, key, defaultValue) {
    var tmp0_safe_receiver = this.p17(descriptor, key);
    if (tmp0_safe_receiver == null)
      null;
    else {
      // Inline function 'kotlin.let' call
      return tmp0_safe_receiver;
    }
    var value = defaultValue();
    this.o17(descriptor, key, value);
    return value;
  };
  protoOf(DescriptorSchemaCache).p17 = function (descriptor, key) {
    var tmp0_safe_receiver = this.t14_1.b2(descriptor);
    var tmp;
    if (tmp0_safe_receiver == null) {
      tmp = null;
    } else {
      tmp = tmp0_safe_receiver.b2(key instanceof Key ? key : THROW_CCE());
    }
    var tmp_0 = tmp;
    return !(tmp_0 == null) ? tmp_0 : null;
  };
  function DiscriminatorHolder(discriminatorToSkip) {
    this.q17_1 = discriminatorToSkip;
  }
  function trySkip($this, $receiver, unknownKey) {
    if ($receiver == null)
      return false;
    if ($receiver.q17_1 === unknownKey) {
      $receiver.q17_1 = null;
      return true;
    }
    return false;
  }
  function skipLeftoverElements($this, descriptor) {
    while (!($this.hj(descriptor) === -1)) {
    }
  }
  function checkLeadingComma($this) {
    if ($this.tz_1.a16() === 4) {
      $this.tz_1.s14('Unexpected leading comma');
    }
  }
  function decodeMapIndex($this) {
    var hasComma = false;
    var decodingKey = !(($this.vz_1 % 2 | 0) === 0);
    if (decodingKey) {
      if (!($this.vz_1 === -1)) {
        hasComma = $this.tz_1.s17();
      }
    } else {
      $this.tz_1.r17(_Char___init__impl__6a9atx(58));
    }
    var tmp;
    if ($this.tz_1.b16()) {
      if (decodingKey) {
        if ($this.vz_1 === -1) {
          var tmp0 = $this.tz_1;
          // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.require' call
          var condition = !hasComma;
          var position = tmp0.a10_1;
          if (!condition) {
            var tmp$ret$1 = 'Unexpected leading comma';
            tmp0.s14(tmp$ret$1, position);
          }
        } else {
          var tmp0_0 = $this.tz_1;
          // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.require' call
          var condition_0 = hasComma;
          var position_0 = tmp0_0.a10_1;
          if (!condition_0) {
            var tmp$ret$3 = 'Expected comma after the key-value pair';
            tmp0_0.s14(tmp$ret$3, position_0);
          }
        }
      }
      $this.vz_1 = $this.vz_1 + 1 | 0;
      tmp = $this.vz_1;
    } else {
      if (hasComma && !$this.rz_1.iz_1.o11_1) {
        invalidTrailingComma($this.tz_1);
      }
      tmp = -1;
    }
    return tmp;
  }
  function coerceInputValue($this, descriptor, index) {
    var tmp0 = $this.rz_1;
    var tmp$ret$0;
    $l$block_2: {
      // Inline function 'kotlinx.serialization.json.internal.tryCoerceValue' call
      var isOptional = descriptor.ch(index);
      var elementDescriptor = descriptor.bh(index);
      var tmp;
      if (isOptional && !elementDescriptor.pg()) {
        tmp = $this.tz_1.t17(true);
      } else {
        tmp = false;
      }
      if (tmp) {
        tmp$ret$0 = true;
        break $l$block_2;
      }
      if (equals(elementDescriptor.ug(), ENUM_getInstance())) {
        var tmp_0;
        if (elementDescriptor.pg()) {
          tmp_0 = $this.tz_1.t17(false);
        } else {
          tmp_0 = false;
        }
        if (tmp_0) {
          tmp$ret$0 = false;
          break $l$block_2;
        }
        var tmp0_elvis_lhs = $this.tz_1.u17($this.xz_1.c11_1);
        var tmp_1;
        if (tmp0_elvis_lhs == null) {
          tmp$ret$0 = false;
          break $l$block_2;
        } else {
          tmp_1 = tmp0_elvis_lhs;
        }
        var enumValue = tmp_1;
        var enumIndex = getJsonNameIndex(elementDescriptor, tmp0, enumValue);
        var coerceToNull = !tmp0.iz_1.f11_1 && elementDescriptor.pg();
        if (enumIndex === -3 && (isOptional || coerceToNull)) {
          $this.tz_1.c16();
          tmp$ret$0 = true;
          break $l$block_2;
        }
      }
      tmp$ret$0 = false;
    }
    return tmp$ret$0;
  }
  function decodeObjectIndex($this, descriptor) {
    var hasComma = $this.tz_1.s17();
    while ($this.tz_1.b16()) {
      hasComma = false;
      var key = decodeStringKey($this);
      $this.tz_1.r17(_Char___init__impl__6a9atx(58));
      var index = getJsonNameIndex(descriptor, $this.rz_1, key);
      var tmp;
      if (!(index === -3)) {
        var tmp_0;
        if ($this.xz_1.h11_1 && coerceInputValue($this, descriptor, index)) {
          hasComma = $this.tz_1.s17();
          tmp_0 = false;
        } else {
          var tmp0_safe_receiver = $this.yz_1;
          if (tmp0_safe_receiver == null)
            null;
          else {
            tmp0_safe_receiver.p14(index);
          }
          return index;
        }
        tmp = tmp_0;
      } else {
        tmp = true;
      }
      var isUnknown = tmp;
      if (isUnknown) {
        hasComma = handleUnknown($this, descriptor, key);
      }
    }
    if (hasComma && !$this.rz_1.iz_1.o11_1) {
      invalidTrailingComma($this.tz_1);
    }
    var tmp1_safe_receiver = $this.yz_1;
    var tmp2_elvis_lhs = tmp1_safe_receiver == null ? null : tmp1_safe_receiver.q14();
    return tmp2_elvis_lhs == null ? -1 : tmp2_elvis_lhs;
  }
  function handleUnknown($this, descriptor, key) {
    if (ignoreUnknownKeys(descriptor, $this.rz_1) || trySkip($this, $this.wz_1, key)) {
      $this.tz_1.w17($this.xz_1.c11_1);
    } else {
      $this.tz_1.b10_1.g15();
      $this.tz_1.v17(key);
    }
    return $this.tz_1.s17();
  }
  function decodeListIndex($this) {
    var hasComma = $this.tz_1.s17();
    var tmp;
    if ($this.tz_1.b16()) {
      if (!($this.vz_1 === -1) && !hasComma) {
        $this.tz_1.s14('Expected end of the array or comma');
      }
      $this.vz_1 = $this.vz_1 + 1 | 0;
      tmp = $this.vz_1;
    } else {
      if (hasComma && !$this.rz_1.iz_1.o11_1) {
        invalidTrailingComma($this.tz_1, 'array');
      }
      tmp = -1;
    }
    return tmp;
  }
  function decodeStringKey($this) {
    var tmp;
    if ($this.xz_1.c11_1) {
      tmp = $this.tz_1.y17();
    } else {
      tmp = $this.tz_1.x17();
    }
    return tmp;
  }
  function StreamingJsonDecoder(json, mode, lexer, descriptor, discriminatorHolder) {
    AbstractDecoder.call(this);
    this.rz_1 = json;
    this.sz_1 = mode;
    this.tz_1 = lexer;
    this.uz_1 = this.rz_1.fj();
    this.vz_1 = -1;
    this.wz_1 = discriminatorHolder;
    this.xz_1 = this.rz_1.iz_1;
    this.yz_1 = this.xz_1.f11_1 ? null : new JsonElementMarker(descriptor);
  }
  protoOf(StreamingJsonDecoder).t11 = function () {
    return this.rz_1;
  };
  protoOf(StreamingJsonDecoder).fj = function () {
    return this.uz_1;
  };
  protoOf(StreamingJsonDecoder).n17 = function () {
    var tmp0_safe_receiver = this.wz_1;
    return tmp0_safe_receiver == null ? null : tmp0_safe_receiver.q17_1;
  };
  protoOf(StreamingJsonDecoder).u11 = function () {
    return (new JsonTreeReader(this.rz_1.iz_1, this.tz_1)).e16();
  };
  protoOf(StreamingJsonDecoder).pi = function (deserializer) {
    var tmp2_withExceptionHandling_this = this.tz_1.b10_1;
    var tmp3_withExceptionHandling_this = this.tz_1;
    // Inline function 'kotlinx.serialization.json.internal.withExceptionHandling' call
    var tmp;
    try {
      var tmp_0;
      if (!(deserializer instanceof AbstractPolymorphicSerializer)) {
        tmp_0 = true;
      } else {
        tmp_0 = this.rz_1.iz_1.i11_1;
      }
      if (tmp_0) {
        return deserializer.qf(this);
      }
      var discriminator = classDiscriminator(deserializer.of(), this.rz_1);
      var tmp0_elvis_lhs = this.tz_1.z17(discriminator, this.xz_1.c11_1);
      var tmp_1;
      if (tmp0_elvis_lhs == null) {
        var tmp2 = isInterface(deserializer, DeserializationStrategy) ? deserializer : THROW_CCE();
        var tmp$ret$2;
        $l$block: {
          // Inline function 'kotlinx.serialization.json.internal.decodeSerializableValuePolymorphic' call
          var tmp_2;
          if (!(tmp2 instanceof AbstractPolymorphicSerializer)) {
            tmp_2 = true;
          } else {
            tmp_2 = this.t11().iz_1.i11_1;
          }
          if (tmp_2) {
            tmp$ret$2 = tmp2.qf(this);
            break $l$block;
          }
          var discriminator_0 = classDiscriminator(tmp2.of(), this.t11());
          var tmp2_0 = this.u11();
          // Inline function 'kotlinx.serialization.json.internal.cast' call
          var serialName = tmp2.of().tg();
          if (!(tmp2_0 instanceof JsonObject)) {
            var tmp2_1 = 'Expected ' + getKClass(JsonObject).m() + ', but had ' + getKClassFromExpression(tmp2_0).m() + ' as the serialized body of ' + serialName;
            // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
            var path = this.tz_1.b10_1.h15();
            var tmp_3;
            if (this.t11().iz_1.r11_1) {
              var tmp$ret$8 = toString(tmp2_0);
              tmp_3 = toString(minify(tmp$ret$8));
            } else {
              tmp_3 = null;
            }
            var inputValue = tmp_3;
            throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_1, path, null, inputValue), tmp2_1, -1, path, inputValue, null);
          }
          var jsonTree = tmp2_0;
          var tmp0_safe_receiver = jsonTree.y11(discriminator_0);
          var tmp1_safe_receiver = tmp0_safe_receiver == null ? null : get_jsonPrimitive(tmp0_safe_receiver);
          var type = tmp1_safe_receiver == null ? null : get_contentOrNull(tmp1_safe_receiver);
          var tmp_4 = tmp2.fg(this, type);
          var actualSerializer = (!(tmp_4 == null) ? isInterface(tmp_4, DeserializationStrategy) : false) ? tmp_4 : null;
          if (actualSerializer == null) {
            var _destruct__k2r9zo = subtypeNotRegisteredMessageJson(type, tmp2.dg());
            var message = _destruct__k2r9zo.ta();
            var hint = _destruct__k2r9zo.ua();
            // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
            var path_0 = this.tz_1.b10_1.h15();
            var tmp_5;
            if (this.t11().iz_1.r11_1) {
              var tmp$ret$13 = jsonTree.toString();
              tmp_5 = toString(minify(tmp$ret$13));
            } else {
              tmp_5 = null;
            }
            var inputValue_0 = tmp_5;
            throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, message, path_0, hint, inputValue_0), message, -1, path_0, inputValue_0, hint);
          }
          tmp$ret$2 = readPolymorphicJson(this.t11(), discriminator_0, jsonTree, actualSerializer);
        }
        return tmp$ret$2;
      } else {
        tmp_1 = tmp0_elvis_lhs;
      }
      var type_0 = tmp_1;
      var tmp_6 = deserializer.fg(this, type_0);
      var actualSerializer_0 = (!(tmp_6 == null) ? isInterface(tmp_6, DeserializationStrategy) : false) ? tmp_6 : null;
      if (actualSerializer_0 == null) {
        var _destruct__k2r9zo_0 = subtypeNotRegisteredMessageJson(type_0, deserializer.dg());
        var message_0 = _destruct__k2r9zo_0.ta();
        var hint_0 = _destruct__k2r9zo_0.ua();
        this.tz_1.s14(message_0, VOID, hint_0);
      }
      this.wz_1 = new DiscriminatorHolder(discriminator);
      tmp = actualSerializer_0.qf(this);
    } catch ($p) {
      var tmp_7;
      if ($p instanceof MissingFieldException) {
        var e = $p;
        if (contains_0(ensureNotNull(e.message), 'at path'))
          throw e;
        var tmp_8 = plus(e.message, ' at path: ');
        var tmp$ret$14 = tmp2_withExceptionHandling_this.h15();
        throw missingFieldExceptionWithNewMessage(e, tmp_8 + tmp$ret$14);
      } else {
        if ($p instanceof SerializationException) {
          var e_0 = $p;
          throw e_0;
        } else {
          if ($p instanceof Exception) {
            var e_1 = $p;
            // Inline function 'kotlinx.serialization.json.internal.errorFromDeserializer' call
            var path_1 = tmp2_withExceptionHandling_this.h15();
            var causeMessage = e_1.message;
            var shortMessage = 'Deserialization failed because of ' + (causeMessage == null ? 'an exception' : "'" + causeMessage + "' exception") + ' in the decoder';
            var tmp_9;
            if (this.t11().iz_1.r11_1) {
              var tmp$ret$19 = tmp3_withExceptionHandling_this.a18();
              tmp_9 = toString(minify(tmp$ret$19));
            } else {
              tmp_9 = null;
            }
            var inputValue_1 = tmp_9;
            throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, shortMessage, path_1, null, inputValue_1), shortMessage, -1, path_1, inputValue_1, null, e_1);
          } else {
            throw $p;
          }
        }
      }
    }
    return tmp;
  };
  protoOf(StreamingJsonDecoder).qi = function (descriptor) {
    var newMode = modeFor(this.rz_1, descriptor);
    this.tz_1.b10_1.c15(descriptor);
    this.tz_1.r17(newMode.l17_1);
    checkLeadingComma(this);
    var tmp;
    switch (newMode.k2_1) {
      case 1:
      case 2:
      case 3:
        tmp = new StreamingJsonDecoder(this.rz_1, newMode, this.tz_1, descriptor, this.wz_1);
        break;
      default:
        var tmp_0;
        if (this.sz_1.equals(newMode) && this.rz_1.iz_1.f11_1) {
          tmp_0 = this;
        } else {
          tmp_0 = new StreamingJsonDecoder(this.rz_1, newMode, this.tz_1, descriptor, this.wz_1);
        }

        tmp = tmp_0;
        break;
    }
    return tmp;
  };
  protoOf(StreamingJsonDecoder).ri = function (descriptor) {
    if (descriptor.wg() === 0 && ignoreUnknownKeys(descriptor, this.rz_1)) {
      skipLeftoverElements(this, descriptor);
    }
    if (this.tz_1.s17() && !this.rz_1.iz_1.o11_1) {
      invalidTrailingComma(this.tz_1, '');
    }
    this.tz_1.r17(this.sz_1.m17_1);
    this.tz_1.b10_1.g15();
  };
  protoOf(StreamingJsonDecoder).bi = function () {
    var tmp;
    var tmp0_safe_receiver = this.yz_1;
    var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.o14_1;
    if (!(tmp1_elvis_lhs == null ? false : tmp1_elvis_lhs)) {
      tmp = !this.tz_1.b18();
    } else {
      tmp = false;
    }
    return tmp;
  };
  protoOf(StreamingJsonDecoder).ci = function () {
    return null;
  };
  protoOf(StreamingJsonDecoder).cj = function (descriptor, index, deserializer, previousValue) {
    var isMapKey = this.sz_1.equals(LexerMode_MAP_getInstance()) && (index & 1) === 0;
    if (isMapKey) {
      this.tz_1.b10_1.f15();
    }
    var value = protoOf(AbstractDecoder).cj.call(this, descriptor, index, deserializer, previousValue);
    if (isMapKey) {
      this.tz_1.b10_1.e15(value);
    }
    return value;
  };
  protoOf(StreamingJsonDecoder).hj = function (descriptor) {
    var index;
    switch (this.sz_1.k2_1) {
      case 0:
        index = decodeObjectIndex(this, descriptor);
        break;
      case 2:
        index = decodeMapIndex(this);
        break;
      default:
        index = decodeListIndex(this);
        break;
    }
    if (!this.sz_1.equals(LexerMode_MAP_getInstance())) {
      this.tz_1.b10_1.d15(index);
    }
    return index;
  };
  protoOf(StreamingJsonDecoder).di = function () {
    return this.tz_1.c18();
  };
  protoOf(StreamingJsonDecoder).ei = function () {
    var value = this.tz_1.d18();
    if (!equalsLong(value, fromInt(convertToByte(value)))) {
      this.tz_1.s14("Failed to parse byte for input '" + value.toString() + "'");
    }
    return convertToByte(value);
  };
  protoOf(StreamingJsonDecoder).fi = function () {
    var value = this.tz_1.d18();
    if (!equalsLong(value, fromInt(convertToShort(value)))) {
      this.tz_1.s14("Failed to parse short for input '" + value.toString() + "'");
    }
    return convertToShort(value);
  };
  protoOf(StreamingJsonDecoder).gi = function () {
    var value = this.tz_1.d18();
    if (!equalsLong(value, fromInt(convertToInt(value)))) {
      this.tz_1.s14("Failed to parse int for input '" + value.toString() + "'");
    }
    return convertToInt(value);
  };
  protoOf(StreamingJsonDecoder).hi = function () {
    return this.tz_1.d18();
  };
  protoOf(StreamingJsonDecoder).ii = function () {
    var tmp0 = this.tz_1;
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.parseString' call
      var input = tmp0.d16();
      try {
        // Inline function 'kotlin.text.toFloat' call
        // Inline function 'kotlin.js.unsafeCast' call
        // Inline function 'kotlin.js.asDynamic' call
        tmp$ret$0 = toDouble(input);
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          tmp0.s14("Failed to parse type '" + 'float' + "' for input '" + input + "'");
        } else {
          throw $p;
        }
      }
    }
    var result = tmp$ret$0;
    var specialFp = this.rz_1.iz_1.k11_1;
    if (specialFp || isFinite(result))
      return result;
    throwInvalidFloatingPointDecoded(this.tz_1, result);
  };
  protoOf(StreamingJsonDecoder).ji = function () {
    var tmp0 = this.tz_1;
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.parseString' call
      var input = tmp0.d16();
      try {
        tmp$ret$0 = toDouble(input);
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          tmp0.s14("Failed to parse type '" + 'double' + "' for input '" + input + "'");
        } else {
          throw $p;
        }
      }
    }
    var result = tmp$ret$0;
    var specialFp = this.rz_1.iz_1.k11_1;
    if (specialFp || isFinite_0(result))
      return result;
    throwInvalidFloatingPointDecoded(this.tz_1, result);
  };
  protoOf(StreamingJsonDecoder).ki = function () {
    var string = this.tz_1.d16();
    if (!(string.length === 1)) {
      this.tz_1.s14("Expected single char, but got '" + string + "'");
    }
    return charCodeAt(string, 0);
  };
  protoOf(StreamingJsonDecoder).li = function () {
    var tmp;
    if (this.xz_1.c11_1) {
      tmp = this.tz_1.y17();
    } else {
      tmp = this.tz_1.c16();
    }
    return tmp;
  };
  protoOf(StreamingJsonDecoder).ni = function (descriptor) {
    return get_isUnsignedNumber(descriptor) ? new JsonDecoderForUnsignedTypes(this.tz_1, this.rz_1) : protoOf(AbstractDecoder).ni.call(this, descriptor);
  };
  protoOf(StreamingJsonDecoder).mi = function (enumDescriptor) {
    return getJsonNameIndexOrThrow(enumDescriptor, this.rz_1, this.li(), ' at path ' + this.tz_1.b10_1.h15());
  };
  function JsonDecoderForUnsignedTypes(lexer, json) {
    AbstractDecoder.call(this);
    this.e18_1 = lexer;
    this.f18_1 = json.fj();
  }
  protoOf(JsonDecoderForUnsignedTypes).fj = function () {
    return this.f18_1;
  };
  protoOf(JsonDecoderForUnsignedTypes).hj = function (descriptor) {
    // Inline function 'kotlin.error' call
    var message = 'unsupported';
    throw IllegalStateException_init_$Create$(toString(message));
  };
  protoOf(JsonDecoderForUnsignedTypes).gi = function () {
    var tmp0 = this.e18_1;
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.parseString' call
      var input = tmp0.d16();
      try {
        // Inline function 'kotlin.UInt.toInt' call
        var this_0 = toUInt(input);
        tmp$ret$0 = _UInt___get_data__impl__f0vqqw(this_0);
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          tmp0.s14("Failed to parse type '" + 'UInt' + "' for input '" + input + "'");
        } else {
          throw $p;
        }
      }
    }
    return tmp$ret$0;
  };
  protoOf(JsonDecoderForUnsignedTypes).hi = function () {
    var tmp0 = this.e18_1;
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.parseString' call
      var input = tmp0.d16();
      try {
        // Inline function 'kotlin.ULong.toLong' call
        var this_0 = toULong(input);
        tmp$ret$0 = _ULong___get_data__impl__fggpzb(this_0);
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          tmp0.s14("Failed to parse type '" + 'ULong' + "' for input '" + input + "'");
        } else {
          throw $p;
        }
      }
    }
    return tmp$ret$0;
  };
  protoOf(JsonDecoderForUnsignedTypes).ei = function () {
    var tmp0 = this.e18_1;
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.parseString' call
      var input = tmp0.d16();
      try {
        // Inline function 'kotlin.UByte.toByte' call
        var this_0 = toUByte(input);
        tmp$ret$0 = _UByte___get_data__impl__jof9qr(this_0);
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          tmp0.s14("Failed to parse type '" + 'UByte' + "' for input '" + input + "'");
        } else {
          throw $p;
        }
      }
    }
    return tmp$ret$0;
  };
  protoOf(JsonDecoderForUnsignedTypes).fi = function () {
    var tmp0 = this.e18_1;
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.parseString' call
      var input = tmp0.d16();
      try {
        // Inline function 'kotlin.UShort.toShort' call
        var this_0 = toUShort(input);
        tmp$ret$0 = _UShort___get_data__impl__g0245(this_0);
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          tmp0.s14("Failed to parse type '" + 'UShort' + "' for input '" + input + "'");
        } else {
          throw $p;
        }
      }
    }
    return tmp$ret$0;
  };
  function get_unsignedNumberDescriptors() {
    _init_properties_StreamingJsonEncoder_kt__pn1bsi();
    return unsignedNumberDescriptors;
  }
  var unsignedNumberDescriptors;
  function StreamingJsonEncoder_init_$Init$(output, json, mode, modeReuseCache, $this) {
    StreamingJsonEncoder.call($this, Composer_0(output, json), json, mode, modeReuseCache);
    return $this;
  }
  function StreamingJsonEncoder_init_$Create$(output, json, mode, modeReuseCache) {
    return StreamingJsonEncoder_init_$Init$(output, json, mode, modeReuseCache, objectCreate(protoOf(StreamingJsonEncoder)));
  }
  function encodeTypeInfo($this, discriminator, serialName) {
    $this.l15_1.m13();
    $this.uj(discriminator);
    $this.l15_1.p13(_Char___init__impl__6a9atx(58));
    $this.l15_1.o13();
    $this.uj(serialName);
  }
  function StreamingJsonEncoder(composer, json, mode, modeReuseCache) {
    AbstractEncoder.call(this);
    this.l15_1 = composer;
    this.m15_1 = json;
    this.n15_1 = mode;
    this.o15_1 = modeReuseCache;
    this.p15_1 = this.m15_1.fj();
    this.q15_1 = this.m15_1.iz_1;
    this.r15_1 = false;
    this.s15_1 = null;
    this.t15_1 = null;
    var i = this.n15_1.k2_1;
    if (!(this.o15_1 == null)) {
      if (!(this.o15_1[i] === null) || !(this.o15_1[i] === this)) {
        this.o15_1[i] = this;
      }
    }
  }
  protoOf(StreamingJsonEncoder).t11 = function () {
    return this.m15_1;
  };
  protoOf(StreamingJsonEncoder).fj = function () {
    return this.p15_1;
  };
  protoOf(StreamingJsonEncoder).nk = function (descriptor, index) {
    return this.q15_1.a11_1;
  };
  protoOf(StreamingJsonEncoder).ik = function (serializer, value) {
    // Inline function 'kotlinx.serialization.json.internal.withExceptionHandling' call
    var tmp;
    try {
      $l$block: {
        // Inline function 'kotlinx.serialization.json.internal.encodePolymorphically' call
        if (this.t11().iz_1.i11_1) {
          serializer.pf(this, value);
          break $l$block;
        }
        var isPolymorphicSerializer = serializer instanceof AbstractPolymorphicSerializer;
        var tmp_0;
        if (isPolymorphicSerializer) {
          tmp_0 = !this.t11().iz_1.q11_1.equals(ClassDiscriminatorMode_NONE_getInstance());
        } else {
          var tmp_1;
          switch (this.t11().iz_1.q11_1.k2_1) {
            case 0:
            case 2:
              tmp_1 = false;
              break;
            case 1:
              // Inline function 'kotlin.let' call

              var it = serializer.of().ug();
              tmp_1 = equals(it, CLASS_getInstance()) || equals(it, OBJECT_getInstance());
              break;
            default:
              noWhenBranchMatchedException();
              break;
          }
          tmp_0 = tmp_1;
        }
        var needDiscriminator = tmp_0;
        var baseClassDiscriminator = needDiscriminator ? classDiscriminator(serializer.of(), this.t11()) : null;
        var tmp_2;
        if (isPolymorphicSerializer) {
          var casted = serializer instanceof AbstractPolymorphicSerializer ? serializer : THROW_CCE();
          $l$block_0: {
            // Inline function 'kotlin.requireNotNull' call
            if (value == null) {
              var message = 'Value for serializer ' + toString(serializer.of()) + ' should always be non-null. Please report issue to the kotlinx.serialization tracker.';
              throw IllegalArgumentException_init_$Create$(toString(message));
            } else {
              break $l$block_0;
            }
          }
          var tmp1_elvis_lhs = casted.gg(this, value);
          var tmp_3;
          if (tmp1_elvis_lhs == null) {
            // Inline function 'kotlin.run' call
            var tmp0_elvis_lhs = getKClassFromExpression(value).m();
            var subClassName = tmp0_elvis_lhs == null ? toString(getKClassFromExpression(value)) : tmp0_elvis_lhs;
            var _destruct__k2r9zo = subtypeNotRegisteredMessageJson(subClassName, casted.dg());
            var message_0 = _destruct__k2r9zo.ta();
            var hint = _destruct__k2r9zo.ua();
            throw new JsonEncodingException(message_0, subClassName, hint);
          } else {
            tmp_3 = tmp1_elvis_lhs;
          }
          var actual = tmp_3;
          tmp_2 = isInterface(actual, SerializationStrategy) ? actual : THROW_CCE();
        } else {
          tmp_2 = serializer;
        }
        var actualSerializer = tmp_2;
        if (!(baseClassDiscriminator == null)) {
          access$checkEncodingConflicts$tJsonPolymorphicHelpersKt(this.t11(), serializer, actualSerializer, baseClassDiscriminator);
          checkKind(actualSerializer.of().ug());
          var serialName = actualSerializer.of().tg();
          this.s15_1 = baseClassDiscriminator;
          this.t15_1 = serialName;
        }
        actualSerializer.pf(this, value);
      }
      tmp = Unit_instance;
    } catch ($p) {
      var tmp_4;
      if ($p instanceof SerializationException) {
        var e = $p;
        throw e;
      } else {
        if ($p instanceof Exception) {
          var e_0 = $p;
          var causeMessage = e_0.message;
          var classSerialName = serializer.of().tg();
          var message_1 = 'Serialization ' + (isBlank(classSerialName) ? '' : "of '" + classSerialName + "' ") + 'failed because of ' + (causeMessage == null ? 'an exception' : "'" + causeMessage + "' exception") + ' in the encoder';
          throw new JsonEncodingException(message_1, classSerialName, VOID, e_0);
        } else {
          throw $p;
        }
      }
    }
    return tmp;
  };
  protoOf(StreamingJsonEncoder).qi = function (descriptor) {
    var newMode = modeFor(this.m15_1, descriptor);
    if (!(newMode.l17_1 === _Char___init__impl__6a9atx(0))) {
      this.l15_1.p13(newMode.l17_1);
      this.l15_1.k13();
    }
    var discriminator = this.s15_1;
    if (!(discriminator == null)) {
      var tmp0_elvis_lhs = this.t15_1;
      encodeTypeInfo(this, discriminator, tmp0_elvis_lhs == null ? descriptor.tg() : tmp0_elvis_lhs);
      this.s15_1 = null;
      this.t15_1 = null;
    }
    if (this.n15_1.equals(newMode)) {
      return this;
    }
    var tmp1_safe_receiver = this.o15_1;
    var tmp2_elvis_lhs = tmp1_safe_receiver == null ? null : tmp1_safe_receiver[newMode.k2_1];
    return tmp2_elvis_lhs == null ? new StreamingJsonEncoder(this.l15_1, this.m15_1, newMode, this.o15_1) : tmp2_elvis_lhs;
  };
  protoOf(StreamingJsonEncoder).ri = function (descriptor) {
    if (!(this.n15_1.m17_1 === _Char___init__impl__6a9atx(0))) {
      this.l15_1.l13();
      this.l15_1.n13();
      this.l15_1.p13(this.n15_1.m17_1);
    }
  };
  protoOf(StreamingJsonEncoder).jj = function (descriptor, index) {
    switch (this.n15_1.k2_1) {
      case 1:
        if (!this.l15_1.j13_1) {
          this.l15_1.p13(_Char___init__impl__6a9atx(44));
        }

        this.l15_1.m13();
        break;
      case 2:
        if (!this.l15_1.j13_1) {
          var tmp = this;
          var tmp_0;
          if ((index % 2 | 0) === 0) {
            this.l15_1.p13(_Char___init__impl__6a9atx(44));
            this.l15_1.m13();
            tmp_0 = true;
          } else {
            this.l15_1.p13(_Char___init__impl__6a9atx(58));
            this.l15_1.o13();
            tmp_0 = false;
          }
          tmp.r15_1 = tmp_0;
        } else {
          this.r15_1 = true;
          this.l15_1.m13();
        }

        break;
      case 3:
        if (index === 0)
          this.r15_1 = true;
        if (index === 1) {
          this.l15_1.p13(_Char___init__impl__6a9atx(44));
          this.l15_1.o13();
          this.r15_1 = false;
        }

        break;
      default:
        if (!this.l15_1.j13_1) {
          this.l15_1.p13(_Char___init__impl__6a9atx(44));
        }

        this.l15_1.m13();
        this.uj(getJsonElementName(descriptor, this.m15_1, index));
        this.l15_1.p13(_Char___init__impl__6a9atx(58));
        this.l15_1.o13();
        break;
    }
    return true;
  };
  protoOf(StreamingJsonEncoder).jk = function (descriptor, index, serializer, value) {
    if (!(value == null) || this.q15_1.f11_1) {
      protoOf(AbstractEncoder).jk.call(this, descriptor, index, serializer, value);
    }
  };
  protoOf(StreamingJsonEncoder).wj = function (descriptor) {
    var tmp;
    if (get_isUnsignedNumber(descriptor)) {
      // Inline function 'kotlinx.serialization.json.internal.StreamingJsonEncoder.composerAs' call
      var tmp_0;
      var tmp_1 = this.l15_1;
      if (tmp_1 instanceof ComposerForUnsignedNumbers) {
        tmp_0 = this.l15_1;
      } else {
        var tmp0 = this.l15_1.i13_1;
        var p1 = this.r15_1;
        tmp_0 = new ComposerForUnsignedNumbers(tmp0, p1);
      }
      var tmp$ret$0 = tmp_0;
      tmp = new StreamingJsonEncoder(tmp$ret$0, this.m15_1, this.n15_1, null);
    } else if (get_isUnquotedLiteral(descriptor)) {
      // Inline function 'kotlinx.serialization.json.internal.StreamingJsonEncoder.composerAs' call
      var tmp_2;
      var tmp_3 = this.l15_1;
      if (tmp_3 instanceof ComposerForUnquotedLiterals) {
        tmp_2 = this.l15_1;
      } else {
        var tmp0_0 = this.l15_1.i13_1;
        var p1_0 = this.r15_1;
        tmp_2 = new ComposerForUnquotedLiterals(tmp0_0, p1_0);
      }
      var tmp$ret$2 = tmp_2;
      tmp = new StreamingJsonEncoder(tmp$ret$2, this.m15_1, this.n15_1, null);
    } else if (!(this.s15_1 == null)) {
      // Inline function 'kotlin.apply' call
      this.t15_1 = descriptor.tg();
      tmp = this;
    } else {
      tmp = protoOf(AbstractEncoder).wj.call(this, descriptor);
    }
    return tmp;
  };
  protoOf(StreamingJsonEncoder).lj = function () {
    this.l15_1.r13('null');
  };
  protoOf(StreamingJsonEncoder).mj = function (value) {
    if (this.r15_1) {
      this.uj(value.toString());
    } else {
      this.l15_1.a14(value);
    }
  };
  protoOf(StreamingJsonEncoder).nj = function (value) {
    if (this.r15_1) {
      this.uj(value.toString());
    } else {
      this.l15_1.v13(value);
    }
  };
  protoOf(StreamingJsonEncoder).oj = function (value) {
    if (this.r15_1) {
      this.uj(value.toString());
    } else {
      this.l15_1.x13(value);
    }
  };
  protoOf(StreamingJsonEncoder).pj = function (value) {
    if (this.r15_1) {
      this.uj(value.toString());
    } else {
      this.l15_1.y13(value);
    }
  };
  protoOf(StreamingJsonEncoder).qj = function (value) {
    if (this.r15_1) {
      this.uj(value.toString());
    } else {
      this.l15_1.z13(value);
    }
  };
  protoOf(StreamingJsonEncoder).rj = function (value) {
    if (this.r15_1) {
      this.uj(value.toString());
    } else {
      this.l15_1.t13(value);
    }
    if (!this.q15_1.k11_1 && !isFinite(value)) {
      throw InvalidFloatingPointEncoded(value);
    }
  };
  protoOf(StreamingJsonEncoder).sj = function (value) {
    if (this.r15_1) {
      this.uj(value.toString());
    } else {
      this.l15_1.u13(value);
    }
    if (!this.q15_1.k11_1 && !isFinite_0(value)) {
      throw InvalidFloatingPointEncoded(value);
    }
  };
  protoOf(StreamingJsonEncoder).tj = function (value) {
    this.uj(toString_1(value));
  };
  protoOf(StreamingJsonEncoder).uj = function (value) {
    return this.l15_1.b14(value);
  };
  protoOf(StreamingJsonEncoder).vj = function (enumDescriptor, index) {
    this.uj(enumDescriptor.yg(index));
  };
  function get_isUnsignedNumber(_this__u8e3s4) {
    _init_properties_StreamingJsonEncoder_kt__pn1bsi();
    return _this__u8e3s4.vg() && get_unsignedNumberDescriptors().v1(_this__u8e3s4);
  }
  function get_isUnquotedLiteral(_this__u8e3s4) {
    _init_properties_StreamingJsonEncoder_kt__pn1bsi();
    return _this__u8e3s4.vg() && equals(_this__u8e3s4, get_jsonUnquotedLiteralDescriptor());
  }
  var properties_initialized_StreamingJsonEncoder_kt_6ifwwk;
  function _init_properties_StreamingJsonEncoder_kt__pn1bsi() {
    if (!properties_initialized_StreamingJsonEncoder_kt_6ifwwk) {
      properties_initialized_StreamingJsonEncoder_kt_6ifwwk = true;
      unsignedNumberDescriptors = setOf([serializer_1(Companion_getInstance_0()).of(), serializer_0(Companion_getInstance()).of(), serializer_2(Companion_getInstance_1()).of(), serializer_3(Companion_getInstance_2()).of()]);
    }
  }
  function get_ESCAPE_STRINGS() {
    _init_properties_StringOps_kt__fcy1db();
    return ESCAPE_STRINGS;
  }
  var ESCAPE_STRINGS;
  var ESCAPE_MARKERS;
  function toHexChar(i) {
    _init_properties_StringOps_kt__fcy1db();
    var d = i & 15;
    var tmp;
    if (d < 10) {
      // Inline function 'kotlin.code' call
      var this_0 = _Char___init__impl__6a9atx(48);
      var tmp$ret$0 = Char__toInt_impl_vasixd(this_0);
      tmp = numberToChar(d + tmp$ret$0 | 0);
    } else {
      var tmp_0 = d - 10 | 0;
      // Inline function 'kotlin.code' call
      var this_1 = _Char___init__impl__6a9atx(97);
      var tmp$ret$1 = Char__toInt_impl_vasixd(this_1);
      tmp = numberToChar(tmp_0 + tmp$ret$1 | 0);
    }
    return tmp;
  }
  function printQuoted(_this__u8e3s4, value) {
    _init_properties_StringOps_kt__fcy1db();
    _this__u8e3s4.q(_Char___init__impl__6a9atx(34));
    var lastPos = 0;
    var inductionVariable = 0;
    var last = charSequenceLength(value) - 1 | 0;
    if (inductionVariable <= last)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        // Inline function 'kotlin.code' call
        var this_0 = charCodeAt(value, i);
        var c = Char__toInt_impl_vasixd(this_0);
        if (c < get_ESCAPE_STRINGS().length && !(get_ESCAPE_STRINGS()[c] == null)) {
          _this__u8e3s4.j9(value, lastPos, i);
          _this__u8e3s4.o(get_ESCAPE_STRINGS()[c]);
          lastPos = i + 1 | 0;
        }
      }
       while (inductionVariable <= last);
    if (!(lastPos === 0))
      _this__u8e3s4.j9(value, lastPos, value.length);
    else
      _this__u8e3s4.o(value);
    _this__u8e3s4.q(_Char___init__impl__6a9atx(34));
  }
  function toBooleanStrictOrNull_0(_this__u8e3s4) {
    _init_properties_StringOps_kt__fcy1db();
    return equals_0(_this__u8e3s4, 'true', true) ? true : equals_0(_this__u8e3s4, 'false', true) ? false : null;
  }
  var properties_initialized_StringOps_kt_wzaea7;
  function _init_properties_StringOps_kt__fcy1db() {
    if (!properties_initialized_StringOps_kt_wzaea7) {
      properties_initialized_StringOps_kt_wzaea7 = true;
      // Inline function 'kotlin.arrayOfNulls' call
      // Inline function 'kotlin.apply' call
      var this_0 = Array(93);
      var inductionVariable = 0;
      if (inductionVariable <= 31)
        do {
          var c = inductionVariable;
          inductionVariable = inductionVariable + 1 | 0;
          var c1 = toHexChar(c >> 12);
          var c2 = toHexChar(c >> 8);
          var c3 = toHexChar(c >> 4);
          var c4 = toHexChar(c);
          this_0[c] = '\\u' + toString_1(c1) + toString_1(c2) + toString_1(c3) + toString_1(c4);
        }
         while (inductionVariable <= 31);
      // Inline function 'kotlin.code' call
      var this_1 = _Char___init__impl__6a9atx(34);
      this_0[Char__toInt_impl_vasixd(this_1)] = '\\"';
      // Inline function 'kotlin.code' call
      var this_2 = _Char___init__impl__6a9atx(92);
      this_0[Char__toInt_impl_vasixd(this_2)] = '\\\\';
      // Inline function 'kotlin.code' call
      var this_3 = _Char___init__impl__6a9atx(9);
      this_0[Char__toInt_impl_vasixd(this_3)] = '\\t';
      // Inline function 'kotlin.code' call
      var this_4 = _Char___init__impl__6a9atx(8);
      this_0[Char__toInt_impl_vasixd(this_4)] = '\\b';
      // Inline function 'kotlin.code' call
      var this_5 = _Char___init__impl__6a9atx(10);
      this_0[Char__toInt_impl_vasixd(this_5)] = '\\n';
      // Inline function 'kotlin.code' call
      var this_6 = _Char___init__impl__6a9atx(13);
      this_0[Char__toInt_impl_vasixd(this_6)] = '\\r';
      this_0[12] = '\\f';
      ESCAPE_STRINGS = this_0;
      // Inline function 'kotlin.apply' call
      var this_7 = new Int8Array(128);
      var inductionVariable_0 = 0;
      if (inductionVariable_0 <= 31)
        do {
          var c_0 = inductionVariable_0;
          inductionVariable_0 = inductionVariable_0 + 1 | 0;
          this_7[c_0] = 1;
        }
         while (inductionVariable_0 <= 31);
      // Inline function 'kotlin.code' call
      var this_8 = _Char___init__impl__6a9atx(34);
      var tmp = Char__toInt_impl_vasixd(this_8);
      // Inline function 'kotlin.code' call
      var this_9 = _Char___init__impl__6a9atx(34);
      var tmp$ret$3 = Char__toInt_impl_vasixd(this_9);
      this_7[tmp] = toByte(tmp$ret$3);
      // Inline function 'kotlin.code' call
      var this_10 = _Char___init__impl__6a9atx(92);
      var tmp_0 = Char__toInt_impl_vasixd(this_10);
      // Inline function 'kotlin.code' call
      var this_11 = _Char___init__impl__6a9atx(92);
      var tmp$ret$5 = Char__toInt_impl_vasixd(this_11);
      this_7[tmp_0] = toByte(tmp$ret$5);
      // Inline function 'kotlin.code' call
      var this_12 = _Char___init__impl__6a9atx(9);
      var tmp_1 = Char__toInt_impl_vasixd(this_12);
      // Inline function 'kotlin.code' call
      var this_13 = _Char___init__impl__6a9atx(116);
      var tmp$ret$7 = Char__toInt_impl_vasixd(this_13);
      this_7[tmp_1] = toByte(tmp$ret$7);
      // Inline function 'kotlin.code' call
      var this_14 = _Char___init__impl__6a9atx(8);
      var tmp_2 = Char__toInt_impl_vasixd(this_14);
      // Inline function 'kotlin.code' call
      var this_15 = _Char___init__impl__6a9atx(98);
      var tmp$ret$9 = Char__toInt_impl_vasixd(this_15);
      this_7[tmp_2] = toByte(tmp$ret$9);
      // Inline function 'kotlin.code' call
      var this_16 = _Char___init__impl__6a9atx(10);
      var tmp_3 = Char__toInt_impl_vasixd(this_16);
      // Inline function 'kotlin.code' call
      var this_17 = _Char___init__impl__6a9atx(110);
      var tmp$ret$11 = Char__toInt_impl_vasixd(this_17);
      this_7[tmp_3] = toByte(tmp$ret$11);
      // Inline function 'kotlin.code' call
      var this_18 = _Char___init__impl__6a9atx(13);
      var tmp_4 = Char__toInt_impl_vasixd(this_18);
      // Inline function 'kotlin.code' call
      var this_19 = _Char___init__impl__6a9atx(114);
      var tmp$ret$13 = Char__toInt_impl_vasixd(this_19);
      this_7[tmp_4] = toByte(tmp$ret$13);
      // Inline function 'kotlin.code' call
      var this_20 = _Char___init__impl__6a9atx(102);
      var tmp$ret$14 = Char__toInt_impl_vasixd(this_20);
      this_7[12] = toByte(tmp$ret$14);
      ESCAPE_MARKERS = this_7;
    }
  }
  function readJson(json, element, deserializer, previousDecoder) {
    previousDecoder = previousDecoder === VOID ? null : previousDecoder;
    var tmp0_safe_receiver = (!(previousDecoder == null) ? isInterface(previousDecoder, PolymorphicJsonDecoder) : false) ? previousDecoder : null;
    var discriminator = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.n17();
    var tmp;
    if (element instanceof JsonObject) {
      tmp = new JsonTreeDecoder(json, element, deserializer.of(), discriminator);
    } else {
      if (element instanceof JsonArray) {
        tmp = new JsonTreeListDecoder(json, element);
      } else {
        var tmp_0;
        if (element instanceof JsonLiteral) {
          tmp_0 = true;
        } else {
          tmp_0 = equals(element, JsonNull_getInstance());
        }
        if (tmp_0) {
          tmp = new JsonPrimitiveDecoder(json, element instanceof JsonPrimitive ? element : THROW_CCE());
        } else {
          noWhenBranchMatchedException();
        }
      }
    }
    var input = tmp;
    return input.pi(deserializer);
  }
  function readPolymorphicJson(_this__u8e3s4, discriminator, element, deserializer) {
    var descriptor = deserializer.of();
    return (new JsonTreeDecoder(_this__u8e3s4, element, descriptor, discriminator, descriptor)).pi(deserializer);
  }
  function unparsedPrimitive($this, literal, primitive, tag) {
    var type = startsWith(primitive, 'i') ? 'an ' + primitive : 'a ' + primitive;
    var tmp2 = "Failed to parse literal '" + literal.toString() + "' as " + type + ' value';
    // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
    var path = $this.m18(tag);
    var tmp;
    if ($this.t11().iz_1.r11_1) {
      var tmp$ret$3 = toString($this.n18());
      tmp = toString(minify(tmp$ret$3));
    } else {
      tmp = null;
    }
    var inputValue = tmp;
    throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, path, null, inputValue), tmp2, -1, path, inputValue, null);
  }
  function AbstractJsonTreeDecoder(json, value, polymorphicDiscriminator) {
    polymorphicDiscriminator = polymorphicDiscriminator === VOID ? null : polymorphicDiscriminator;
    NamedValueDecoder.call(this);
    this.i18_1 = json;
    this.j18_1 = value;
    this.k18_1 = polymorphicDiscriminator;
    this.l18_1 = this.t11().iz_1;
  }
  protoOf(AbstractJsonTreeDecoder).t11 = function () {
    return this.i18_1;
  };
  protoOf(AbstractJsonTreeDecoder).y1 = function () {
    return this.j18_1;
  };
  protoOf(AbstractJsonTreeDecoder).fj = function () {
    return this.t11().fj();
  };
  protoOf(AbstractJsonTreeDecoder).n17 = function () {
    return this.k18_1;
  };
  protoOf(AbstractJsonTreeDecoder).n18 = function () {
    var tmp0_safe_receiver = this.pw();
    var tmp;
    if (tmp0_safe_receiver == null) {
      tmp = null;
    } else {
      // Inline function 'kotlin.let' call
      tmp = this.o18(tmp0_safe_receiver);
    }
    var tmp1_elvis_lhs = tmp;
    return tmp1_elvis_lhs == null ? this.y1() : tmp1_elvis_lhs;
  };
  protoOf(AbstractJsonTreeDecoder).m18 = function (currentTag) {
    return this.rw() + ('.' + currentTag);
  };
  protoOf(AbstractJsonTreeDecoder).u11 = function () {
    return this.n18();
  };
  protoOf(AbstractJsonTreeDecoder).pi = function (deserializer) {
    var tmp2_withExceptionHandling_this = this.n18();
    // Inline function 'kotlinx.serialization.json.internal.withExceptionHandling' call
    var tmp;
    try {
      var tmp$ret$2;
      $l$block: {
        // Inline function 'kotlinx.serialization.json.internal.decodeSerializableValuePolymorphic' call
        var tmp_0;
        if (!(deserializer instanceof AbstractPolymorphicSerializer)) {
          tmp_0 = true;
        } else {
          tmp_0 = this.t11().iz_1.i11_1;
        }
        if (tmp_0) {
          tmp$ret$2 = deserializer.qf(this);
          break $l$block;
        }
        var discriminator = classDiscriminator(deserializer.of(), this.t11());
        var tmp2 = this.u11();
        // Inline function 'kotlinx.serialization.json.internal.cast' call
        var serialName = deserializer.of().tg();
        if (!(tmp2 instanceof JsonObject)) {
          var tmp2_0 = 'Expected ' + getKClass(JsonObject).m() + ', but had ' + getKClassFromExpression(tmp2).m() + ' as the serialized body of ' + serialName;
          // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
          var path = this.rw();
          var tmp_1;
          if (this.t11().iz_1.r11_1) {
            var tmp$ret$8 = toString(tmp2);
            tmp_1 = toString(minify(tmp$ret$8));
          } else {
            tmp_1 = null;
          }
          var inputValue = tmp_1;
          throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_0, path, null, inputValue), tmp2_0, -1, path, inputValue, null);
        }
        var jsonTree = tmp2;
        var tmp0_safe_receiver = jsonTree.y11(discriminator);
        var tmp1_safe_receiver = tmp0_safe_receiver == null ? null : get_jsonPrimitive(tmp0_safe_receiver);
        var type = tmp1_safe_receiver == null ? null : get_contentOrNull(tmp1_safe_receiver);
        var tmp_2 = deserializer.fg(this, type);
        var actualSerializer = (!(tmp_2 == null) ? isInterface(tmp_2, DeserializationStrategy) : false) ? tmp_2 : null;
        if (actualSerializer == null) {
          var _destruct__k2r9zo = subtypeNotRegisteredMessageJson(type, deserializer.dg());
          var message = _destruct__k2r9zo.ta();
          var hint = _destruct__k2r9zo.ua();
          // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
          var path_0 = this.rw();
          var tmp_3;
          if (this.t11().iz_1.r11_1) {
            var tmp$ret$13 = jsonTree.toString();
            tmp_3 = toString(minify(tmp$ret$13));
          } else {
            tmp_3 = null;
          }
          var inputValue_0 = tmp_3;
          throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, message, path_0, hint, inputValue_0), message, -1, path_0, inputValue_0, hint);
        }
        tmp$ret$2 = readPolymorphicJson(this.t11(), discriminator, jsonTree, actualSerializer);
      }
      tmp = tmp$ret$2;
    } catch ($p) {
      var tmp_4;
      if ($p instanceof MissingFieldException) {
        var e = $p;
        if (contains_0(ensureNotNull(e.message), 'at path'))
          throw e;
        var tmp_5 = plus(e.message, ' at path: ');
        var tmp$ret$14 = this.rw();
        throw missingFieldExceptionWithNewMessage(e, tmp_5 + tmp$ret$14);
      } else {
        if ($p instanceof SerializationException) {
          var e_0 = $p;
          throw e_0;
        } else {
          if ($p instanceof Exception) {
            var e_1 = $p;
            // Inline function 'kotlinx.serialization.json.internal.errorFromDeserializer' call
            var path_1 = this.rw();
            var causeMessage = e_1.message;
            var shortMessage = 'Deserialization failed because of ' + (causeMessage == null ? 'an exception' : "'" + causeMessage + "' exception") + ' in the decoder';
            var tmp_6;
            if (this.t11().iz_1.r11_1) {
              var tmp$ret$19 = toString(tmp2_withExceptionHandling_this);
              tmp_6 = toString(minify(tmp$ret$19));
            } else {
              tmp_6 = null;
            }
            var inputValue_1 = tmp_6;
            throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, shortMessage, path_1, null, inputValue_1), shortMessage, -1, path_1, inputValue_1, null, e_1);
          } else {
            throw $p;
          }
        }
      }
    }
    return tmp;
  };
  protoOf(AbstractJsonTreeDecoder).qw = function (parentName, childName) {
    return childName;
  };
  protoOf(AbstractJsonTreeDecoder).qi = function (descriptor) {
    var currentObject = this.n18();
    var tmp0_subject = descriptor.ug();
    var tmp;
    var tmp_0;
    if (equals(tmp0_subject, LIST_getInstance())) {
      tmp_0 = true;
    } else {
      tmp_0 = tmp0_subject instanceof PolymorphicKind;
    }
    if (tmp_0) {
      var tmp_1 = this.t11();
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
      // Inline function 'kotlinx.serialization.json.internal.cast' call
      var serialName = descriptor.tg();
      if (!(currentObject instanceof JsonArray)) {
        var tmp2 = 'Expected ' + getKClass(JsonArray).m() + ', but had ' + getKClassFromExpression(currentObject).m() + ' as the serialized body of ' + serialName;
        // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
        var path = this.rw();
        var tmp_2;
        if (this.t11().iz_1.r11_1) {
          var tmp$ret$6 = toString(currentObject);
          tmp_2 = toString(minify(tmp$ret$6));
        } else {
          tmp_2 = null;
        }
        var inputValue = tmp_2;
        throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, path, null, inputValue), tmp2, -1, path, inputValue, null);
      }
      tmp = new JsonTreeListDecoder(tmp_1, currentObject);
    } else {
      if (equals(tmp0_subject, MAP_getInstance())) {
        // Inline function 'kotlinx.serialization.json.internal.selectMapMode' call
        var this_0 = this.t11();
        var keyDescriptor = carrierDescriptor(descriptor.bh(0), this_0.fj());
        var keyKind = keyDescriptor.ug();
        var tmp_3;
        var tmp_4;
        if (keyKind instanceof PrimitiveKind) {
          tmp_4 = true;
        } else {
          tmp_4 = equals(keyKind, ENUM_getInstance());
        }
        if (tmp_4) {
          var tmp_5 = this.t11();
          // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
          // Inline function 'kotlinx.serialization.json.internal.cast' call
          var serialName_0 = descriptor.tg();
          if (!(currentObject instanceof JsonObject)) {
            var tmp2_0 = 'Expected ' + getKClass(JsonObject).m() + ', but had ' + getKClassFromExpression(currentObject).m() + ' as the serialized body of ' + serialName_0;
            // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
            var path_0 = this.rw();
            var tmp_6;
            if (this.t11().iz_1.r11_1) {
              var tmp$ret$15 = toString(currentObject);
              tmp_6 = toString(minify(tmp$ret$15));
            } else {
              tmp_6 = null;
            }
            var inputValue_0 = tmp_6;
            throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_0, path_0, null, inputValue_0), tmp2_0, -1, path_0, inputValue_0, null);
          }
          tmp_3 = new JsonTreeMapDecoder(tmp_5, currentObject, descriptor);
        } else {
          if (this_0.iz_1.d11_1) {
            var tmp_7 = this.t11();
            // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
            // Inline function 'kotlinx.serialization.json.internal.cast' call
            var serialName_1 = descriptor.tg();
            if (!(currentObject instanceof JsonArray)) {
              var tmp2_1 = 'Expected ' + getKClass(JsonArray).m() + ', but had ' + getKClassFromExpression(currentObject).m() + ' as the serialized body of ' + serialName_1;
              // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
              var path_1 = this.rw();
              var tmp_8;
              if (this.t11().iz_1.r11_1) {
                var tmp$ret$23 = toString(currentObject);
                tmp_8 = toString(minify(tmp$ret$23));
              } else {
                tmp_8 = null;
              }
              var inputValue_1 = tmp_8;
              throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_1, path_1, null, inputValue_1), tmp2_1, -1, path_1, inputValue_1, null);
            }
            tmp_3 = new JsonTreeListDecoder(tmp_7, currentObject);
          } else {
            throw InvalidKeyKindException(keyDescriptor);
          }
        }
        tmp = tmp_3;
      } else {
        var tmp_9 = this.t11();
        // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
        // Inline function 'kotlinx.serialization.json.internal.cast' call
        var serialName_2 = descriptor.tg();
        if (!(currentObject instanceof JsonObject)) {
          var tmp2_2 = 'Expected ' + getKClass(JsonObject).m() + ', but had ' + getKClassFromExpression(currentObject).m() + ' as the serialized body of ' + serialName_2;
          // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
          var path_2 = this.rw();
          var tmp_10;
          if (this.t11().iz_1.r11_1) {
            var tmp$ret$30 = toString(currentObject);
            tmp_10 = toString(minify(tmp$ret$30));
          } else {
            tmp_10 = null;
          }
          var inputValue_2 = tmp_10;
          throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_2, path_2, null, inputValue_2), tmp2_2, -1, path_2, inputValue_2, null);
        }
        tmp = new JsonTreeDecoder(tmp_9, currentObject, descriptor, this.k18_1);
      }
    }
    return tmp;
  };
  protoOf(AbstractJsonTreeDecoder).ri = function (descriptor) {
  };
  protoOf(AbstractJsonTreeDecoder).bi = function () {
    var tmp = this.n18();
    return !(tmp instanceof JsonNull);
  };
  protoOf(AbstractJsonTreeDecoder).p18 = function (tag, enumDescriptor) {
    var tmp = this.t11();
    // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.getPrimitiveValue' call
    var tmp2 = this.o18(tag);
    // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
    // Inline function 'kotlinx.serialization.json.internal.cast' call
    var serialName = enumDescriptor.tg();
    if (!(tmp2 instanceof JsonPrimitive)) {
      var tmp2_0 = 'Expected ' + getKClass(JsonPrimitive).m() + ', but had ' + getKClassFromExpression(tmp2).m() + ' as the serialized body of ' + serialName;
      // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
      var path = this.m18(tag);
      var tmp_0;
      if (this.t11().iz_1.r11_1) {
        var tmp$ret$7 = toString(tmp2);
        tmp_0 = toString(minify(tmp$ret$7));
      } else {
        tmp_0 = null;
      }
      var inputValue = tmp_0;
      throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_0, path, null, inputValue), tmp2_0, -1, path, inputValue, null);
    }
    return getJsonNameIndexOrThrow(enumDescriptor, tmp, tmp2.v11());
  };
  protoOf(AbstractJsonTreeDecoder).dx = function (tag, enumDescriptor) {
    return this.p18((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE(), enumDescriptor);
  };
  protoOf(AbstractJsonTreeDecoder).q18 = function (tag) {
    return !(this.o18(tag) === JsonNull_getInstance());
  };
  protoOf(AbstractJsonTreeDecoder).tw = function (tag) {
    return this.q18((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE());
  };
  protoOf(AbstractJsonTreeDecoder).r18 = function (tag) {
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.getPrimitiveValue' call
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
      // Inline function 'kotlinx.serialization.json.internal.cast' call
      var value = this.o18(tag);
      if (!(value instanceof JsonPrimitive)) {
        var tmp2 = 'Expected ' + getKClass(JsonPrimitive).m() + ', but had ' + getKClassFromExpression(value).m() + ' as the serialized body of ' + 'boolean';
        // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
        var path = this.m18(tag);
        var tmp;
        if (this.t11().iz_1.r11_1) {
          var tmp$ret$7 = toString(value);
          tmp = toString(minify(tmp$ret$7));
        } else {
          tmp = null;
        }
        var inputValue = tmp;
        throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, path, null, inputValue), tmp2, -1, path, inputValue, null);
      }
      var literal = value;
      try {
        var tmp0_elvis_lhs = get_booleanOrNull(literal);
        var tmp_0;
        if (tmp0_elvis_lhs == null) {
          unparsedPrimitive(this, literal, 'boolean', tag);
        } else {
          tmp_0 = tmp0_elvis_lhs;
        }
        tmp$ret$0 = tmp_0;
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          unparsedPrimitive(this, literal, 'boolean', tag);
        } else {
          throw $p;
        }
      }
    }
    return tmp$ret$0;
  };
  protoOf(AbstractJsonTreeDecoder).uw = function (tag) {
    return this.r18((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE());
  };
  protoOf(AbstractJsonTreeDecoder).s18 = function (tag) {
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.getPrimitiveValue' call
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
      // Inline function 'kotlinx.serialization.json.internal.cast' call
      var value = this.o18(tag);
      if (!(value instanceof JsonPrimitive)) {
        var tmp2 = 'Expected ' + getKClass(JsonPrimitive).m() + ', but had ' + getKClassFromExpression(value).m() + ' as the serialized body of ' + 'byte';
        // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
        var path = this.m18(tag);
        var tmp;
        if (this.t11().iz_1.r11_1) {
          var tmp$ret$7 = toString(value);
          tmp = toString(minify(tmp$ret$7));
        } else {
          tmp = null;
        }
        var inputValue = tmp;
        throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, path, null, inputValue), tmp2, -1, path, inputValue, null);
      }
      var literal = value;
      try {
        var result = parseLongImpl(literal);
        var tmp_0;
        // Inline function 'kotlin.ranges.contains' call
        var this_0 = numberRangeToNumber(-128, 127);
        if (contains(isInterface(this_0, ClosedRange) ? this_0 : THROW_CCE(), result)) {
          tmp_0 = convertToByte(result);
        } else {
          tmp_0 = null;
        }
        var tmp0_elvis_lhs = tmp_0;
        var tmp_1;
        if (tmp0_elvis_lhs == null) {
          unparsedPrimitive(this, literal, 'byte', tag);
        } else {
          tmp_1 = tmp0_elvis_lhs;
        }
        tmp$ret$0 = tmp_1;
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          unparsedPrimitive(this, literal, 'byte', tag);
        } else {
          throw $p;
        }
      }
    }
    return tmp$ret$0;
  };
  protoOf(AbstractJsonTreeDecoder).vw = function (tag) {
    return this.s18((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE());
  };
  protoOf(AbstractJsonTreeDecoder).t18 = function (tag) {
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.getPrimitiveValue' call
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
      // Inline function 'kotlinx.serialization.json.internal.cast' call
      var value = this.o18(tag);
      if (!(value instanceof JsonPrimitive)) {
        var tmp2 = 'Expected ' + getKClass(JsonPrimitive).m() + ', but had ' + getKClassFromExpression(value).m() + ' as the serialized body of ' + 'short';
        // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
        var path = this.m18(tag);
        var tmp;
        if (this.t11().iz_1.r11_1) {
          var tmp$ret$7 = toString(value);
          tmp = toString(minify(tmp$ret$7));
        } else {
          tmp = null;
        }
        var inputValue = tmp;
        throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, path, null, inputValue), tmp2, -1, path, inputValue, null);
      }
      var literal = value;
      try {
        var result = parseLongImpl(literal);
        var tmp_0;
        // Inline function 'kotlin.ranges.contains' call
        var this_0 = numberRangeToNumber(-32768, 32767);
        if (contains(isInterface(this_0, ClosedRange) ? this_0 : THROW_CCE(), result)) {
          tmp_0 = convertToShort(result);
        } else {
          tmp_0 = null;
        }
        var tmp0_elvis_lhs = tmp_0;
        var tmp_1;
        if (tmp0_elvis_lhs == null) {
          unparsedPrimitive(this, literal, 'short', tag);
        } else {
          tmp_1 = tmp0_elvis_lhs;
        }
        tmp$ret$0 = tmp_1;
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          unparsedPrimitive(this, literal, 'short', tag);
        } else {
          throw $p;
        }
      }
    }
    return tmp$ret$0;
  };
  protoOf(AbstractJsonTreeDecoder).ww = function (tag) {
    return this.t18((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE());
  };
  protoOf(AbstractJsonTreeDecoder).u18 = function (tag) {
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.getPrimitiveValue' call
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
      // Inline function 'kotlinx.serialization.json.internal.cast' call
      var value = this.o18(tag);
      if (!(value instanceof JsonPrimitive)) {
        var tmp2 = 'Expected ' + getKClass(JsonPrimitive).m() + ', but had ' + getKClassFromExpression(value).m() + ' as the serialized body of ' + 'int';
        // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
        var path = this.m18(tag);
        var tmp;
        if (this.t11().iz_1.r11_1) {
          var tmp$ret$7 = toString(value);
          tmp = toString(minify(tmp$ret$7));
        } else {
          tmp = null;
        }
        var inputValue = tmp;
        throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, path, null, inputValue), tmp2, -1, path, inputValue, null);
      }
      var literal = value;
      try {
        var result = parseLongImpl(literal);
        var tmp_0;
        // Inline function 'kotlin.ranges.contains' call
        var this_0 = numberRangeToNumber(-2147483648, 2147483647);
        if (contains(isInterface(this_0, ClosedRange) ? this_0 : THROW_CCE(), result)) {
          tmp_0 = convertToInt(result);
        } else {
          tmp_0 = null;
        }
        var tmp0_elvis_lhs = tmp_0;
        var tmp_1;
        if (tmp0_elvis_lhs == null) {
          unparsedPrimitive(this, literal, 'int', tag);
        } else {
          tmp_1 = tmp0_elvis_lhs;
        }
        tmp$ret$0 = tmp_1;
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          unparsedPrimitive(this, literal, 'int', tag);
        } else {
          throw $p;
        }
      }
    }
    return tmp$ret$0;
  };
  protoOf(AbstractJsonTreeDecoder).xw = function (tag) {
    return this.u18((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE());
  };
  protoOf(AbstractJsonTreeDecoder).v18 = function (tag) {
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.getPrimitiveValue' call
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
      // Inline function 'kotlinx.serialization.json.internal.cast' call
      var value = this.o18(tag);
      if (!(value instanceof JsonPrimitive)) {
        var tmp2 = 'Expected ' + getKClass(JsonPrimitive).m() + ', but had ' + getKClassFromExpression(value).m() + ' as the serialized body of ' + 'long';
        // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
        var path = this.m18(tag);
        var tmp;
        if (this.t11().iz_1.r11_1) {
          var tmp$ret$7 = toString(value);
          tmp = toString(minify(tmp$ret$7));
        } else {
          tmp = null;
        }
        var inputValue = tmp;
        throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, path, null, inputValue), tmp2, -1, path, inputValue, null);
      }
      var literal = value;
      try {
        var tmp0_elvis_lhs = parseLongImpl(literal);
        var tmp_0;
        if (tmp0_elvis_lhs == null) {
          unparsedPrimitive(this, literal, 'long', tag);
        } else {
          tmp_0 = tmp0_elvis_lhs;
        }
        tmp$ret$0 = tmp_0;
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          unparsedPrimitive(this, literal, 'long', tag);
        } else {
          throw $p;
        }
      }
    }
    return tmp$ret$0;
  };
  protoOf(AbstractJsonTreeDecoder).yw = function (tag) {
    return this.v18((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE());
  };
  protoOf(AbstractJsonTreeDecoder).w18 = function (tag) {
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.getPrimitiveValue' call
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
      // Inline function 'kotlinx.serialization.json.internal.cast' call
      var value = this.o18(tag);
      if (!(value instanceof JsonPrimitive)) {
        var tmp2 = 'Expected ' + getKClass(JsonPrimitive).m() + ', but had ' + getKClassFromExpression(value).m() + ' as the serialized body of ' + 'float';
        // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
        var path = this.m18(tag);
        var tmp;
        if (this.t11().iz_1.r11_1) {
          var tmp$ret$7 = toString(value);
          tmp = toString(minify(tmp$ret$7));
        } else {
          tmp = null;
        }
        var inputValue = tmp;
        throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, path, null, inputValue), tmp2, -1, path, inputValue, null);
      }
      var literal = value;
      try {
        var tmp0_elvis_lhs = get_float(literal);
        var tmp_0;
        if (tmp0_elvis_lhs == null) {
          unparsedPrimitive(this, literal, 'float', tag);
        } else {
          tmp_0 = tmp0_elvis_lhs;
        }
        tmp$ret$0 = tmp_0;
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          unparsedPrimitive(this, literal, 'float', tag);
        } else {
          throw $p;
        }
      }
    }
    var result = tmp$ret$0;
    var specialFp = this.t11().iz_1.k11_1;
    if (specialFp || isFinite(result))
      return result;
    // Inline function 'kotlinx.serialization.json.internal.InvalidFloatingPointDecoded' call
    var tmp2_0 = access$nonFiniteFpMessage$tJsonExceptionsKt(result, tag);
    // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
    var hint = "It is possible to deserialize them using 'JsonBuilder.allowSpecialFloatingPointValues = true'";
    var tmp_1;
    if (this.t11().iz_1.r11_1) {
      var tmp$ret$13 = toString(this.n18());
      tmp_1 = toString(minify(tmp$ret$13));
    } else {
      tmp_1 = null;
    }
    var inputValue_0 = tmp_1;
    throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_0, null, hint, inputValue_0), tmp2_0, -1, null, inputValue_0, hint);
  };
  protoOf(AbstractJsonTreeDecoder).zw = function (tag) {
    return this.w18((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE());
  };
  protoOf(AbstractJsonTreeDecoder).x18 = function (tag) {
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.getPrimitiveValue' call
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
      // Inline function 'kotlinx.serialization.json.internal.cast' call
      var value = this.o18(tag);
      if (!(value instanceof JsonPrimitive)) {
        var tmp2 = 'Expected ' + getKClass(JsonPrimitive).m() + ', but had ' + getKClassFromExpression(value).m() + ' as the serialized body of ' + 'double';
        // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
        var path = this.m18(tag);
        var tmp;
        if (this.t11().iz_1.r11_1) {
          var tmp$ret$7 = toString(value);
          tmp = toString(minify(tmp$ret$7));
        } else {
          tmp = null;
        }
        var inputValue = tmp;
        throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, path, null, inputValue), tmp2, -1, path, inputValue, null);
      }
      var literal = value;
      try {
        var tmp0_elvis_lhs = get_double(literal);
        var tmp_0;
        if (tmp0_elvis_lhs == null) {
          unparsedPrimitive(this, literal, 'double', tag);
        } else {
          tmp_0 = tmp0_elvis_lhs;
        }
        tmp$ret$0 = tmp_0;
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          unparsedPrimitive(this, literal, 'double', tag);
        } else {
          throw $p;
        }
      }
    }
    var result = tmp$ret$0;
    var specialFp = this.t11().iz_1.k11_1;
    if (specialFp || isFinite_0(result))
      return result;
    // Inline function 'kotlinx.serialization.json.internal.InvalidFloatingPointDecoded' call
    var tmp2_0 = access$nonFiniteFpMessage$tJsonExceptionsKt(result, tag);
    // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
    var hint = "It is possible to deserialize them using 'JsonBuilder.allowSpecialFloatingPointValues = true'";
    var tmp_1;
    if (this.t11().iz_1.r11_1) {
      var tmp$ret$13 = toString(this.n18());
      tmp_1 = toString(minify(tmp$ret$13));
    } else {
      tmp_1 = null;
    }
    var inputValue_0 = tmp_1;
    throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_0, null, hint, inputValue_0), tmp2_0, -1, null, inputValue_0, hint);
  };
  protoOf(AbstractJsonTreeDecoder).ax = function (tag) {
    return this.x18((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE());
  };
  protoOf(AbstractJsonTreeDecoder).y18 = function (tag) {
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.getPrimitiveValue' call
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
      // Inline function 'kotlinx.serialization.json.internal.cast' call
      var value = this.o18(tag);
      if (!(value instanceof JsonPrimitive)) {
        var tmp2 = 'Expected ' + getKClass(JsonPrimitive).m() + ', but had ' + getKClassFromExpression(value).m() + ' as the serialized body of ' + 'char';
        // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
        var path = this.m18(tag);
        var tmp;
        if (this.t11().iz_1.r11_1) {
          var tmp$ret$7 = toString(value);
          tmp = toString(minify(tmp$ret$7));
        } else {
          tmp = null;
        }
        var inputValue = tmp;
        throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, path, null, inputValue), tmp2, -1, path, inputValue, null);
      }
      var literal = value;
      try {
        var tmp0_elvis_lhs = new Char(single(literal.v11()));
        var tmp_0;
        if (tmp0_elvis_lhs == null) {
          unparsedPrimitive(this, literal, 'char', tag);
        } else {
          tmp_0 = tmp0_elvis_lhs;
        }
        tmp$ret$0 = tmp_0.j1_1;
        break $l$block;
      } catch ($p) {
        if ($p instanceof IllegalArgumentException) {
          var e = $p;
          unparsedPrimitive(this, literal, 'char', tag);
        } else {
          throw $p;
        }
      }
    }
    return tmp$ret$0;
  };
  protoOf(AbstractJsonTreeDecoder).bx = function (tag) {
    return this.y18((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE());
  };
  protoOf(AbstractJsonTreeDecoder).z18 = function (tag) {
    // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
    // Inline function 'kotlinx.serialization.json.internal.cast' call
    var value = this.o18(tag);
    if (!(value instanceof JsonPrimitive)) {
      var tmp2 = 'Expected ' + getKClass(JsonPrimitive).m() + ', but had ' + getKClassFromExpression(value).m() + ' as the serialized body of ' + 'string';
      // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
      var path = this.m18(tag);
      var tmp;
      if (this.t11().iz_1.r11_1) {
        var tmp$ret$6 = toString(value);
        tmp = toString(minify(tmp$ret$6));
      } else {
        tmp = null;
      }
      var inputValue = tmp;
      throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, path, null, inputValue), tmp2, -1, path, inputValue, null);
    }
    var value_0 = value;
    if (!(value_0 instanceof JsonLiteral)) {
      var tmp2_0 = "Expected string value for a non-null key '" + tag + "', got null literal instead";
      var tmp4 = this.m18(tag);
      // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
      var hint = "Use 'coerceInputValues = true' in 'Json {}' builder to coerce nulls if property has a default value.";
      var tmp_0;
      if (this.t11().iz_1.r11_1) {
        var tmp$ret$10 = toString(this.n18());
        tmp_0 = toString(minify(tmp$ret$10));
      } else {
        tmp_0 = null;
      }
      var inputValue_0 = tmp_0;
      throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_0, tmp4, hint, inputValue_0), tmp2_0, -1, tmp4, inputValue_0, hint);
    }
    if (!value_0.c12_1 && !this.t11().iz_1.c11_1) {
      var tmp2_1 = "String literal for value of key '" + tag + "' should be quoted";
      var tmp4_0 = this.m18(tag);
      // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
      var hint_0 = "Use 'isLenient = true' in 'Json {}' builder to accept non-compliant JSON.";
      var tmp_1;
      if (this.t11().iz_1.r11_1) {
        var tmp$ret$14 = toString(this.n18());
        tmp_1 = toString(minify(tmp$ret$14));
      } else {
        tmp_1 = null;
      }
      var inputValue_1 = tmp_1;
      throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_1, tmp4_0, hint_0, inputValue_1), tmp2_1, -1, tmp4_0, inputValue_1, hint_0);
    }
    return value_0.e12_1;
  };
  protoOf(AbstractJsonTreeDecoder).cx = function (tag) {
    return this.z18((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE());
  };
  protoOf(AbstractJsonTreeDecoder).a19 = function (tag, inlineDescriptor) {
    var tmp;
    if (get_isUnsignedNumber(inlineDescriptor)) {
      var tmp_0 = this.t11();
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.getPrimitiveValue' call
      var tmp2 = this.o18(tag);
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
      // Inline function 'kotlinx.serialization.json.internal.cast' call
      var serialName = inlineDescriptor.tg();
      if (!(tmp2 instanceof JsonPrimitive)) {
        var tmp2_0 = 'Expected ' + getKClass(JsonPrimitive).m() + ', but had ' + getKClassFromExpression(tmp2).m() + ' as the serialized body of ' + serialName;
        // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
        var path = this.m18(tag);
        var tmp_1;
        if (this.t11().iz_1.r11_1) {
          var tmp$ret$7 = toString(tmp2);
          tmp_1 = toString(minify(tmp$ret$7));
        } else {
          tmp_1 = null;
        }
        var inputValue = tmp_1;
        throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_0, path, null, inputValue), tmp2_0, -1, path, inputValue, null);
      }
      var lexer = StringJsonLexer_0(tmp_0, tmp2.v11());
      tmp = new JsonDecoderForUnsignedTypes(lexer, this.t11());
    } else {
      tmp = protoOf(NamedValueDecoder).ex.call(this, tag, inlineDescriptor);
    }
    return tmp;
  };
  protoOf(AbstractJsonTreeDecoder).ex = function (tag, inlineDescriptor) {
    return this.a19((!(tag == null) ? typeof tag === 'string' : false) ? tag : THROW_CCE(), inlineDescriptor);
  };
  protoOf(AbstractJsonTreeDecoder).ni = function (descriptor) {
    return !(this.pw() == null) ? protoOf(NamedValueDecoder).ni.call(this, descriptor) : (new JsonPrimitiveDecoder(this.t11(), this.y1(), this.k18_1)).ni(descriptor);
  };
  function coerceInputValue_0($this, descriptor, index, element) {
    var tmp0 = $this.t11();
    var tmp$ret$0;
    $l$block_2: {
      // Inline function 'kotlinx.serialization.json.internal.tryCoerceValue' call
      var isOptional = descriptor.ch(index);
      var elementDescriptor = descriptor.bh(index);
      var tmp;
      if (isOptional && !elementDescriptor.pg()) {
        tmp = element instanceof JsonNull;
      } else {
        tmp = false;
      }
      if (tmp) {
        tmp$ret$0 = true;
        break $l$block_2;
      }
      if (equals(elementDescriptor.ug(), ENUM_getInstance())) {
        var tmp_0;
        if (elementDescriptor.pg()) {
          tmp_0 = element instanceof JsonNull;
        } else {
          tmp_0 = false;
        }
        if (tmp_0) {
          tmp$ret$0 = false;
          break $l$block_2;
        }
        var tmp0_safe_receiver = element instanceof JsonPrimitive ? element : null;
        var tmp0_elvis_lhs = tmp0_safe_receiver == null ? null : get_contentOrNull(tmp0_safe_receiver);
        var tmp_1;
        if (tmp0_elvis_lhs == null) {
          tmp$ret$0 = false;
          break $l$block_2;
        } else {
          tmp_1 = tmp0_elvis_lhs;
        }
        var enumValue = tmp_1;
        var enumIndex = getJsonNameIndex(elementDescriptor, tmp0, enumValue);
        var coerceToNull = !tmp0.iz_1.f11_1 && elementDescriptor.pg();
        if (enumIndex === -3 && (isOptional || coerceToNull)) {
          tmp$ret$0 = true;
          break $l$block_2;
        }
      }
      tmp$ret$0 = false;
    }
    return tmp$ret$0;
  }
  function throwUnknownKey($this, key) {
    var tmp2 = "Encountered an unknown key '" + key + "'";
    var tmp4 = $this.rw();
    // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
    var hint = "Use 'ignoreUnknownKeys = true' in 'Json {}' builder or '@JsonIgnoreUnknownKeys' annotation to ignore unknown keys.";
    var tmp;
    if ($this.t11().iz_1.r11_1) {
      var tmp$ret$3 = $this.y1().toString();
      tmp = toString(minify(tmp$ret$3));
    } else {
      tmp = null;
    }
    var inputValue = tmp;
    throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2, tmp4, hint, inputValue), tmp2, -1, tmp4, inputValue, hint);
  }
  function JsonTreeDecoder(json, value, descriptor, polymorphicDiscriminator, polyDescriptor) {
    polymorphicDiscriminator = polymorphicDiscriminator === VOID ? null : polymorphicDiscriminator;
    polyDescriptor = polyDescriptor === VOID ? null : polyDescriptor;
    AbstractJsonTreeDecoder.call(this, json, value, polymorphicDiscriminator);
    this.h19_1 = value;
    this.i19_1 = polyDescriptor;
    this.j19_1 = this.y1().e2().r();
    this.k19_1 = this.l18_1.f11_1 ? null : new JsonElementMarker(descriptor);
    this.l19_1 = null;
    this.m19_1 = null;
  }
  protoOf(JsonTreeDecoder).y1 = function () {
    return this.h19_1;
  };
  protoOf(JsonTreeDecoder).hj = function (descriptor) {
    var entries = this.j19_1;
    $l$loop: while (entries.s()) {
      var entry = entries.t();
      var key = entry.x1();
      var index = getJsonNameIndex(descriptor, this.t11(), key);
      if (!(index === -3)) {
        if (this.l18_1.h11_1 && coerceInputValue_0(this, descriptor, index, entry.y1())) {
          continue $l$loop;
        }
        var tmp0_safe_receiver = this.k19_1;
        if (tmp0_safe_receiver == null)
          null;
        else {
          tmp0_safe_receiver.p14(index);
        }
        this.l19_1 = key;
        this.m19_1 = entry.y1();
        return index;
      }
      if (!(key === this.k18_1) && !ignoreUnknownKeys(descriptor, this.t11())) {
        throwUnknownKey(this, key);
      }
    }
    var tmp1_safe_receiver = this.k19_1;
    var tmp2_elvis_lhs = tmp1_safe_receiver == null ? null : tmp1_safe_receiver.q14();
    var markerIndex = tmp2_elvis_lhs == null ? -1 : tmp2_elvis_lhs;
    if (!(markerIndex === -1)) {
      this.l19_1 = descriptor.yg(markerIndex);
      this.m19_1 = null;
    }
    return markerIndex;
  };
  protoOf(JsonTreeDecoder).bi = function () {
    var tmp;
    var tmp0_safe_receiver = this.k19_1;
    var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.o14_1;
    if (!(tmp1_elvis_lhs == null ? false : tmp1_elvis_lhs)) {
      tmp = protoOf(AbstractJsonTreeDecoder).bi.call(this);
    } else {
      tmp = false;
    }
    return tmp;
  };
  protoOf(JsonTreeDecoder).lw = function (descriptor, index) {
    var tmp0_elvis_lhs = this.l19_1;
    return tmp0_elvis_lhs == null ? descriptor.yg(index) : tmp0_elvis_lhs;
  };
  protoOf(JsonTreeDecoder).o18 = function (tag) {
    var tmp0_elvis_lhs = this.m19_1;
    return tmp0_elvis_lhs == null ? getValue(this.y1(), tag) : tmp0_elvis_lhs;
  };
  protoOf(JsonTreeDecoder).qi = function (descriptor) {
    if (descriptor === this.i19_1) {
      var tmp = this.t11();
      var tmp2 = this.n18();
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonTreeDecoder.cast' call
      // Inline function 'kotlinx.serialization.json.internal.cast' call
      var serialName = this.i19_1.tg();
      if (!(tmp2 instanceof JsonObject)) {
        var tmp2_0 = 'Expected ' + getKClass(JsonObject).m() + ', but had ' + getKClassFromExpression(tmp2).m() + ' as the serialized body of ' + serialName;
        // Inline function 'kotlinx.serialization.json.internal.decodingExceptionOf' call
        var path = this.rw();
        var tmp_0;
        if (this.t11().iz_1.r11_1) {
          var tmp$ret$6 = toString(tmp2);
          tmp_0 = toString(minify(tmp$ret$6));
        } else {
          tmp_0 = null;
        }
        var inputValue = tmp_0;
        throw new JsonDecodingException(access$formatDecodingException$tJsonExceptionsKt(-1, tmp2_0, path, null, inputValue), tmp2_0, -1, path, inputValue, null);
      }
      return new JsonTreeDecoder(tmp, tmp2, descriptor, this.k18_1, this.i19_1);
    }
    return protoOf(AbstractJsonTreeDecoder).qi.call(this, descriptor);
  };
  function JsonTreeListDecoder(json, value) {
    AbstractJsonTreeDecoder.call(this, json, value);
    this.t19_1 = value;
    this.u19_1 = this.t19_1.x();
    this.v19_1 = -1;
  }
  protoOf(JsonTreeListDecoder).y1 = function () {
    return this.t19_1;
  };
  protoOf(JsonTreeListDecoder).lw = function (descriptor, index) {
    return index.toString();
  };
  protoOf(JsonTreeListDecoder).o18 = function (tag) {
    return this.t19_1.y(toInt(tag));
  };
  protoOf(JsonTreeListDecoder).hj = function (descriptor) {
    while (this.v19_1 < (this.u19_1 - 1 | 0)) {
      this.v19_1 = this.v19_1 + 1 | 0;
      return this.v19_1;
    }
    return -1;
  };
  function JsonPrimitiveDecoder(json, value, polymorphicDiscriminator) {
    polymorphicDiscriminator = polymorphicDiscriminator === VOID ? null : polymorphicDiscriminator;
    AbstractJsonTreeDecoder.call(this, json, value, polymorphicDiscriminator);
    this.c1a_1 = value;
    this.fx('primitive');
  }
  protoOf(JsonPrimitiveDecoder).y1 = function () {
    return this.c1a_1;
  };
  protoOf(JsonPrimitiveDecoder).hj = function (descriptor) {
    return 0;
  };
  protoOf(JsonPrimitiveDecoder).o18 = function (tag) {
    // Inline function 'kotlin.require' call
    if (!(tag === 'primitive')) {
      var message = "This input can only handle primitives with 'primitive' tag";
      throw IllegalArgumentException_init_$Create$(toString(message));
    }
    return this.c1a_1;
  };
  function JsonTreeMapDecoder(json, value, descriptor) {
    JsonTreeDecoder.call(this, json, value, descriptor);
    this.p1a_1 = toList(value.c2());
    this.q1a_1 = imul(this.p1a_1.x(), 2);
    this.r1a_1 = -1;
  }
  protoOf(JsonTreeMapDecoder).lw = function (descriptor, index) {
    var i = index / 2 | 0;
    return this.p1a_1.y(i);
  };
  protoOf(JsonTreeMapDecoder).hj = function (descriptor) {
    while (this.r1a_1 < (this.q1a_1 - 1 | 0)) {
      this.r1a_1 = this.r1a_1 + 1 | 0;
      return this.r1a_1;
    }
    return -1;
  };
  protoOf(JsonTreeMapDecoder).o18 = function (tag) {
    return (this.r1a_1 % 2 | 0) === 0 ? JsonPrimitive_0(tag) : getValue(this.y1(), tag);
  };
  protoOf(JsonTreeMapDecoder).ri = function (descriptor) {
  };
  function appendEscape($this, lastPosition, current) {
    $this.s1a(lastPosition, current);
    return appendEsc($this, current + 1 | 0);
  }
  function decodedString($this, lastPosition, currentPosition) {
    $this.s1a(lastPosition, currentPosition);
    var result = $this.d10_1.toString();
    $this.d10_1.o9(0);
    return result;
  }
  function takePeeked($this) {
    // Inline function 'kotlin.also' call
    var this_0 = ensureNotNull($this.c10_1);
    $this.c10_1 = null;
    return this_0;
  }
  function wasUnquotedString($this) {
    return !(charSequenceGet($this.a18(), $this.a10_1 - 1 | 0) === _Char___init__impl__6a9atx(34));
  }
  function appendEsc($this, startPosition) {
    var currentPosition = startPosition;
    currentPosition = $this.t1a(currentPosition);
    if (currentPosition === -1) {
      $this.s14('Expected escape sequence to continue, got EOF');
    }
    var tmp = $this.a18();
    var _unary__edvuaz = currentPosition;
    currentPosition = _unary__edvuaz + 1 | 0;
    var currentChar = charSequenceGet(tmp, _unary__edvuaz);
    if (currentChar === _Char___init__impl__6a9atx(117)) {
      return appendHex($this, $this.a18(), currentPosition);
    }
    // Inline function 'kotlin.code' call
    var tmp$ret$0 = Char__toInt_impl_vasixd(currentChar);
    var c = escapeToChar(tmp$ret$0);
    if (c === _Char___init__impl__6a9atx(0)) {
      $this.s14("Invalid escaped char '" + toString_1(currentChar) + "'");
    }
    $this.d10_1.q(c);
    return currentPosition;
  }
  function appendHex($this, source, startPos) {
    if ((startPos + 4 | 0) >= charSequenceLength(source)) {
      $this.a10_1 = startPos;
      $this.u1a();
      if (($this.a10_1 + 4 | 0) >= charSequenceLength(source)) {
        $this.s14('Unexpected EOF during unicode escape');
      }
      return appendHex($this, source, $this.a10_1);
    }
    $this.d10_1.q(numberToChar((((fromHexChar($this, source, startPos) << 12) + (fromHexChar($this, source, startPos + 1 | 0) << 8) | 0) + (fromHexChar($this, source, startPos + 2 | 0) << 4) | 0) + fromHexChar($this, source, startPos + 3 | 0) | 0));
    return startPos + 4 | 0;
  }
  function fromHexChar($this, source, currentPosition) {
    var character = charSequenceGet(source, currentPosition);
    var tmp;
    if (_Char___init__impl__6a9atx(48) <= character ? character <= _Char___init__impl__6a9atx(57) : false) {
      // Inline function 'kotlin.code' call
      var tmp_0 = Char__toInt_impl_vasixd(character);
      // Inline function 'kotlin.code' call
      var this_0 = _Char___init__impl__6a9atx(48);
      tmp = tmp_0 - Char__toInt_impl_vasixd(this_0) | 0;
    } else if (_Char___init__impl__6a9atx(97) <= character ? character <= _Char___init__impl__6a9atx(102) : false) {
      // Inline function 'kotlin.code' call
      var tmp_1 = Char__toInt_impl_vasixd(character);
      // Inline function 'kotlin.code' call
      var this_1 = _Char___init__impl__6a9atx(97);
      tmp = (tmp_1 - Char__toInt_impl_vasixd(this_1) | 0) + 10 | 0;
    } else if (_Char___init__impl__6a9atx(65) <= character ? character <= _Char___init__impl__6a9atx(70) : false) {
      // Inline function 'kotlin.code' call
      var tmp_2 = Char__toInt_impl_vasixd(character);
      // Inline function 'kotlin.code' call
      var this_2 = _Char___init__impl__6a9atx(65);
      tmp = (tmp_2 - Char__toInt_impl_vasixd(this_2) | 0) + 10 | 0;
    } else {
      $this.s14("Invalid toHexChar char '" + toString_1(character) + "' in unicode escape");
    }
    return tmp;
  }
  function consumeBoolean2($this, start) {
    var current = $this.t1a(start);
    if (current >= charSequenceLength($this.a18()) || current === -1) {
      $this.s14('EOF');
    }
    var tmp = $this.a18();
    var _unary__edvuaz = current;
    current = _unary__edvuaz + 1 | 0;
    // Inline function 'kotlin.code' call
    var this_0 = charSequenceGet(tmp, _unary__edvuaz);
    var tmp0_subject = Char__toInt_impl_vasixd(this_0) | 32;
    var tmp_0;
    // Inline function 'kotlin.code' call
    var this_1 = _Char___init__impl__6a9atx(116);
    if (tmp0_subject === Char__toInt_impl_vasixd(this_1)) {
      consumeBooleanLiteral($this, 'rue', current);
      tmp_0 = true;
    } else {
      // Inline function 'kotlin.code' call
      var this_2 = _Char___init__impl__6a9atx(102);
      if (tmp0_subject === Char__toInt_impl_vasixd(this_2)) {
        consumeBooleanLiteral($this, 'alse', current);
        tmp_0 = false;
      } else {
        $this.s14("Expected valid boolean literal prefix, but had '" + $this.d16() + "'");
      }
    }
    return tmp_0;
  }
  function consumeBooleanLiteral($this, literalSuffix, current) {
    if ((charSequenceLength($this.a18()) - current | 0) < literalSuffix.length) {
      $this.s14('Unexpected end of boolean literal');
    }
    var inductionVariable = 0;
    var last = charSequenceLength(literalSuffix) - 1 | 0;
    if (inductionVariable <= last)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        var expected = charCodeAt(literalSuffix, i);
        var actual = charSequenceGet($this.a18(), current + i | 0);
        // Inline function 'kotlin.code' call
        var tmp = Char__toInt_impl_vasixd(expected);
        // Inline function 'kotlin.code' call
        if (!(tmp === (Char__toInt_impl_vasixd(actual) | 32))) {
          $this.s14("Expected valid boolean literal prefix, but had '" + $this.d16() + "'");
        }
      }
       while (inductionVariable <= last);
    $this.a10_1 = current + literalSuffix.length | 0;
  }
  function consumeNumericLiteral$calculateExponent(exponentAccumulator, isExponentPositive) {
    var tmp;
    switch (isExponentPositive) {
      case false:
        // Inline function 'kotlin.math.pow' call

        var x = -toNumber(exponentAccumulator);
        tmp = Math.pow(10.0, x);
        break;
      case true:
        // Inline function 'kotlin.math.pow' call

        var x_0 = toNumber(exponentAccumulator);
        tmp = Math.pow(10.0, x_0);
        break;
      default:
        noWhenBranchMatchedException();
        break;
    }
    return tmp;
  }
  function AbstractJsonLexer(configuration) {
    this.zz_1 = configuration;
    this.a10_1 = 0;
    this.b10_1 = new JsonPath(this.zz_1);
    this.c10_1 = null;
    this.d10_1 = StringBuilder_init_$Create$();
  }
  protoOf(AbstractJsonLexer).u1a = function () {
  };
  protoOf(AbstractJsonLexer).s17 = function () {
    var current = this.v1a();
    var source = this.a18();
    if (current >= charSequenceLength(source) || current === -1)
      return false;
    if (charSequenceGet(source, current) === _Char___init__impl__6a9atx(44)) {
      this.a10_1 = this.a10_1 + 1 | 0;
      return true;
    }
    return false;
  };
  protoOf(AbstractJsonLexer).w1a = function (c) {
    return c === _Char___init__impl__6a9atx(125) || c === _Char___init__impl__6a9atx(93) || (c === _Char___init__impl__6a9atx(58) || c === _Char___init__impl__6a9atx(44)) ? false : true;
  };
  protoOf(AbstractJsonLexer).e10 = function () {
    var nextToken = this.f16();
    if (!(nextToken === 10)) {
      this.s14('Expected EOF after parsing, but had ' + toString_1(charSequenceGet(this.a18(), this.a10_1 - 1 | 0)) + ' instead');
    }
  };
  protoOf(AbstractJsonLexer).z15 = function (expected) {
    var token = this.f16();
    if (!(token === expected)) {
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.fail' call
      var expected_0 = tokenDescription(expected);
      var position = true && this.a10_1 > 0 ? this.a10_1 - 1 | 0 : this.a10_1;
      var s = this.a10_1 === charSequenceLength(this.a18()) || position < 0 ? 'EOF' : toString_1(charSequenceGet(this.a18(), position));
      var tmp$ret$1 = 'Expected ' + expected_0 + ", but had '" + s + "' instead";
      this.s14(tmp$ret$1, position);
    }
    return token;
  };
  protoOf(AbstractJsonLexer).x1a = function (expected) {
    if (this.a10_1 > 0 && expected === _Char___init__impl__6a9atx(34)) {
      var tmp$ret$0;
      $l$block: {
        // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.withPositionRollback' call
        var snapshot = this.a10_1;
        try {
          this.a10_1 = this.a10_1 - 1 | 0;
          tmp$ret$0 = this.d16();
          break $l$block;
        }finally {
          this.a10_1 = snapshot;
        }
      }
      var inputLiteral = tmp$ret$0;
      if (inputLiteral === 'null') {
        this.r14("Expected string literal but 'null' literal was found", this.a10_1 - 1 | 0, "Use 'coerceInputValues = true' in 'Json {}' builder to coerce nulls if property has a default value.");
      }
    }
    // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.fail' call
    var expectedToken = charToTokenClass(expected);
    var expected_0 = tokenDescription(expectedToken);
    var position = true && this.a10_1 > 0 ? this.a10_1 - 1 | 0 : this.a10_1;
    var s = this.a10_1 === charSequenceLength(this.a18()) || position < 0 ? 'EOF' : toString_1(charSequenceGet(this.a18(), position));
    var tmp$ret$3 = 'Expected ' + expected_0 + ", but had '" + s + "' instead";
    this.s14(tmp$ret$3, position);
  };
  protoOf(AbstractJsonLexer).a16 = function () {
    var source = this.a18();
    var cpos = this.a10_1;
    $l$loop_0: while (true) {
      cpos = this.t1a(cpos);
      if (cpos === -1)
        break $l$loop_0;
      var ch = charSequenceGet(source, cpos);
      if (ch === _Char___init__impl__6a9atx(32) || ch === _Char___init__impl__6a9atx(10) || ch === _Char___init__impl__6a9atx(13) || ch === _Char___init__impl__6a9atx(9)) {
        cpos = cpos + 1 | 0;
        continue $l$loop_0;
      }
      this.a10_1 = cpos;
      return charToTokenClass(ch);
    }
    this.a10_1 = cpos;
    return 10;
  };
  protoOf(AbstractJsonLexer).t17 = function (doConsume) {
    var current = this.v1a();
    current = this.t1a(current);
    var len = charSequenceLength(this.a18()) - current | 0;
    if (len < 4 || current === -1)
      return false;
    var inductionVariable = 0;
    if (inductionVariable <= 3)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (!(charCodeAt('null', i) === charSequenceGet(this.a18(), current + i | 0)))
          return false;
      }
       while (inductionVariable <= 3);
    if (len > 4 && charToTokenClass(charSequenceGet(this.a18(), current + 4 | 0)) === 0)
      return false;
    if (doConsume) {
      this.a10_1 = current + 4 | 0;
    }
    return true;
  };
  protoOf(AbstractJsonLexer).b18 = function (doConsume, $super) {
    doConsume = doConsume === VOID ? true : doConsume;
    return $super === VOID ? this.t17(doConsume) : $super.t17.call(this, doConsume);
  };
  protoOf(AbstractJsonLexer).u17 = function (isLenient) {
    var token = this.a16();
    var tmp;
    if (isLenient) {
      if (!(token === 1) && !(token === 0))
        return null;
      tmp = this.d16();
    } else {
      if (!(token === 1))
        return null;
      tmp = this.c16();
    }
    var string = tmp;
    this.c10_1 = string;
    return string;
  };
  protoOf(AbstractJsonLexer).y1a = function () {
    this.c10_1 = null;
  };
  protoOf(AbstractJsonLexer).z1a = function (startPos, endPos) {
    // Inline function 'kotlin.text.substring' call
    var this_0 = this.a18();
    return toString(charSequenceSubSequence(this_0, startPos, endPos));
  };
  protoOf(AbstractJsonLexer).c16 = function () {
    if (!(this.c10_1 == null)) {
      return takePeeked(this);
    }
    return this.x17();
  };
  protoOf(AbstractJsonLexer).consumeString2 = function (source, startPosition, current) {
    var currentPosition = current;
    var lastPosition = startPosition;
    var char = charSequenceGet(source, currentPosition);
    var usedAppend = false;
    while (!(char === _Char___init__impl__6a9atx(34))) {
      if (char === _Char___init__impl__6a9atx(92)) {
        usedAppend = true;
        currentPosition = this.t1a(appendEscape(this, lastPosition, currentPosition));
        if (currentPosition === -1) {
          this.s14('Unexpected EOF', currentPosition);
        }
        lastPosition = currentPosition;
      } else {
        currentPosition = currentPosition + 1 | 0;
        if (currentPosition >= charSequenceLength(source)) {
          usedAppend = true;
          this.s1a(lastPosition, currentPosition);
          currentPosition = this.t1a(currentPosition);
          if (currentPosition === -1) {
            this.s14('Unexpected EOF', currentPosition);
          }
          lastPosition = currentPosition;
        }
      }
      char = charSequenceGet(source, currentPosition);
    }
    var tmp;
    if (!usedAppend) {
      tmp = this.z1a(lastPosition, currentPosition);
    } else {
      tmp = decodedString(this, lastPosition, currentPosition);
    }
    var string = tmp;
    this.a10_1 = currentPosition + 1 | 0;
    return string;
  };
  protoOf(AbstractJsonLexer).y17 = function () {
    var result = this.d16();
    if (result === 'null' && wasUnquotedString(this)) {
      this.s14("Unexpected 'null' value instead of string literal");
    }
    return result;
  };
  protoOf(AbstractJsonLexer).d16 = function () {
    if (!(this.c10_1 == null)) {
      return takePeeked(this);
    }
    var current = this.v1a();
    if (current >= charSequenceLength(this.a18()) || current === -1) {
      this.s14('EOF', current);
    }
    var token = charToTokenClass(charSequenceGet(this.a18(), current));
    if (token === 1) {
      return this.c16();
    }
    if (!(token === 0)) {
      this.s14('Expected beginning of the string, but got ' + toString_1(charSequenceGet(this.a18(), current)));
    }
    var usedAppend = false;
    while (charToTokenClass(charSequenceGet(this.a18(), current)) === 0) {
      current = current + 1 | 0;
      if (current >= charSequenceLength(this.a18())) {
        usedAppend = true;
        this.s1a(this.a10_1, current);
        var eof = this.t1a(current);
        if (eof === -1) {
          this.a10_1 = current;
          return decodedString(this, 0, 0);
        } else {
          current = eof;
        }
      }
    }
    var tmp;
    if (!usedAppend) {
      tmp = this.z1a(this.a10_1, current);
    } else {
      tmp = decodedString(this, this.a10_1, current);
    }
    var result = tmp;
    this.a10_1 = current;
    return result;
  };
  protoOf(AbstractJsonLexer).s1a = function (fromIndex, toIndex) {
    this.d10_1.j9(this.a18(), fromIndex, toIndex);
  };
  protoOf(AbstractJsonLexer).w17 = function (allowLenientStrings) {
    // Inline function 'kotlin.collections.mutableListOf' call
    var tokenStack = ArrayList_init_$Create$();
    var lastToken = this.a16();
    if (!(lastToken === 8) && !(lastToken === 6)) {
      this.d16();
      return Unit_instance;
    }
    $l$loop: while (true) {
      lastToken = this.a16();
      if (lastToken === 1) {
        if (allowLenientStrings)
          this.d16();
        else
          this.x17();
        continue $l$loop;
      }
      var tmp0_subject = lastToken;
      if (tmp0_subject === 8 || tmp0_subject === 6) {
        tokenStack.w(lastToken);
      } else if (tmp0_subject === 9) {
        if (!(last(tokenStack) === 8)) {
          this.s14('found ] instead of }');
        }
        removeLast(tokenStack);
      } else if (tmp0_subject === 7) {
        if (!(last(tokenStack) === 6)) {
          this.s14('found } instead of ]');
        }
        removeLast(tokenStack);
      } else if (tmp0_subject === 10) {
        this.s14('Unexpected end of input due to malformed JSON during ignoring unknown keys');
      }
      this.f16();
      if (tokenStack.x() === 0)
        return Unit_instance;
    }
  };
  protoOf(AbstractJsonLexer).toString = function () {
    return "JsonReader(source='" + toString(this.a18()) + "', currentPosition=" + this.a10_1 + ')';
  };
  protoOf(AbstractJsonLexer).v17 = function (key) {
    var processed = this.z1a(0, this.a10_1);
    var lastIndexOf_0 = lastIndexOf(processed, key);
    this.r14("Encountered an unknown key '" + key + "'", lastIndexOf_0, "Use 'ignoreUnknownKeys = true' in 'Json {}' builder or '@JsonIgnoreUnknownKeys' annotation to ignore unknown keys.");
  };
  protoOf(AbstractJsonLexer).r14 = function (message, position, hint) {
    throw decodingExceptionOf_0(this, message, position, this.b10_1.h15(), hint, this.a18());
  };
  protoOf(AbstractJsonLexer).s14 = function (message, position, hint, $super) {
    position = position === VOID ? this.a10_1 : position;
    hint = hint === VOID ? null : hint;
    return $super === VOID ? this.r14(message, position, hint) : $super.r14.call(this, message, position, hint);
  };
  protoOf(AbstractJsonLexer).d18 = function () {
    var current = this.v1a();
    current = this.t1a(current);
    if (current >= charSequenceLength(this.a18()) || current === -1) {
      this.s14('EOF');
    }
    var tmp;
    if (charSequenceGet(this.a18(), current) === _Char___init__impl__6a9atx(34)) {
      current = current + 1 | 0;
      if (current === charSequenceLength(this.a18())) {
        this.s14('EOF');
      }
      tmp = true;
    } else {
      tmp = false;
    }
    var hasQuotation = tmp;
    var accumulator = new Long(0, 0);
    var exponentAccumulator = new Long(0, 0);
    var isNegative = false;
    var isExponentPositive = false;
    var hasExponent = false;
    var start = current;
    $l$loop_4: while (!(current === charSequenceLength(this.a18()))) {
      var ch = charSequenceGet(this.a18(), current);
      if ((ch === _Char___init__impl__6a9atx(101) || ch === _Char___init__impl__6a9atx(69)) && !hasExponent) {
        if (current === start) {
          this.s14("Unexpected symbol '" + toString_1(ch) + "' in numeric literal", current);
        }
        isExponentPositive = true;
        hasExponent = true;
        current = current + 1 | 0;
        continue $l$loop_4;
      }
      if (ch === _Char___init__impl__6a9atx(45) && hasExponent) {
        if (current === start) {
          this.s14("Unexpected symbol '-' in numeric literal", current);
        }
        isExponentPositive = false;
        current = current + 1 | 0;
        continue $l$loop_4;
      }
      if (ch === _Char___init__impl__6a9atx(43) && hasExponent) {
        if (current === start) {
          this.s14("Unexpected symbol '+' in numeric literal", current);
        }
        isExponentPositive = true;
        current = current + 1 | 0;
        continue $l$loop_4;
      }
      if (ch === _Char___init__impl__6a9atx(45)) {
        if (!(current === start)) {
          this.s14("Unexpected symbol '-' in numeric literal", current);
        }
        isNegative = true;
        current = current + 1 | 0;
        continue $l$loop_4;
      }
      var token = charToTokenClass(ch);
      if (!(token === 0))
        break $l$loop_4;
      current = current + 1 | 0;
      var digit = Char__minus_impl_a2frrh(ch, _Char___init__impl__6a9atx(48));
      if (!(0 <= digit ? digit <= 9 : false)) {
        this.s14("Unexpected symbol '" + toString_1(ch) + "' in numeric literal", current - 1 | 0);
      }
      if (hasExponent) {
        // Inline function 'kotlin.Long.times' call
        var this_0 = exponentAccumulator;
        // Inline function 'kotlin.Long.plus' call
        var this_1 = multiply(this_0, fromInt(10));
        exponentAccumulator = add(this_1, fromInt(digit));
        continue $l$loop_4;
      }
      // Inline function 'kotlin.Long.times' call
      var this_2 = accumulator;
      // Inline function 'kotlin.Long.minus' call
      var this_3 = multiply(this_2, fromInt(10));
      accumulator = subtract(this_3, fromInt(digit));
      if (compare(accumulator, new Long(0, 0)) > 0) {
        this.s14('Numeric value overflow');
      }
    }
    var hasChars = !(current === start);
    if (start === current || (isNegative && start === (current - 1 | 0))) {
      this.s14('Expected numeric literal', current);
    }
    if (hasQuotation) {
      if (!hasChars) {
        this.s14('EOF');
      }
      if (!(charSequenceGet(this.a18(), current) === _Char___init__impl__6a9atx(34))) {
        this.s14('Expected closing quotation mark', current);
      }
      current = current + 1 | 0;
    }
    this.a10_1 = current;
    if (hasExponent) {
      var doubleAccumulator = toNumber(accumulator) * consumeNumericLiteral$calculateExponent(exponentAccumulator, isExponentPositive);
      if (doubleAccumulator > toNumber(new Long(-1, 2147483647)) || doubleAccumulator < toNumber(new Long(0, -2147483648))) {
        this.s14('Numeric value overflow');
      }
      // Inline function 'kotlin.math.floor' call
      if (!(Math.floor(doubleAccumulator) === doubleAccumulator)) {
        this.s14("Can't convert " + doubleAccumulator + ' to Long');
      }
      accumulator = numberToLong(doubleAccumulator);
    }
    var tmp_0;
    if (isNegative) {
      tmp_0 = accumulator;
    } else if (!equalsLong(accumulator, new Long(0, -2147483648))) {
      tmp_0 = negate(accumulator);
    } else {
      this.s14('Numeric value overflow');
    }
    return tmp_0;
  };
  protoOf(AbstractJsonLexer).z11 = function () {
    var result = this.d18();
    var next = this.f16();
    if (!(next === 10)) {
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.fail' call
      var expected = tokenDescription(10);
      var position = true && this.a10_1 > 0 ? this.a10_1 - 1 | 0 : this.a10_1;
      var s = this.a10_1 === charSequenceLength(this.a18()) || position < 0 ? 'EOF' : toString_1(charSequenceGet(this.a18(), position));
      var tmp$ret$1 = "Expected input to contain a single valid number, but got '" + s + "' after it";
      this.s14(tmp$ret$1, position);
    }
    return result;
  };
  protoOf(AbstractJsonLexer).c18 = function () {
    var current = this.v1a();
    if (current === charSequenceLength(this.a18())) {
      this.s14('EOF');
    }
    var tmp;
    if (charSequenceGet(this.a18(), current) === _Char___init__impl__6a9atx(34)) {
      current = current + 1 | 0;
      tmp = true;
    } else {
      tmp = false;
    }
    var hasQuotation = tmp;
    var result = consumeBoolean2(this, current);
    if (hasQuotation) {
      if (this.a10_1 === charSequenceLength(this.a18())) {
        this.s14('EOF');
      }
      if (!(charSequenceGet(this.a18(), this.a10_1) === _Char___init__impl__6a9atx(34))) {
        this.s14('Expected closing quotation mark');
      }
      this.a10_1 = this.a10_1 + 1 | 0;
    }
    return result;
  };
  function charToTokenClass(c) {
    var tmp;
    // Inline function 'kotlin.code' call
    if (Char__toInt_impl_vasixd(c) < 126) {
      var tmp_0 = CharMappings_getInstance().b1b_1;
      // Inline function 'kotlin.code' call
      tmp = tmp_0[Char__toInt_impl_vasixd(c)];
    } else {
      tmp = 0;
    }
    return tmp;
  }
  function tokenDescription(token) {
    return token === 1 ? "quotation mark '\"'" : token === 2 ? "string escape sequence '\\'" : token === 4 ? "comma ','" : token === 5 ? "colon ':'" : token === 6 ? "start of the object '{'" : token === 7 ? "end of the object '}'" : token === 8 ? "start of the array '['" : token === 9 ? "end of the array ']'" : token === 10 ? 'end of the input' : token === 127 ? 'invalid token' : 'valid token';
  }
  function escapeToChar(c) {
    return c < 117 ? CharMappings_getInstance().a1b_1[c] : _Char___init__impl__6a9atx(0);
  }
  function initEscape($this) {
    var inductionVariable = 0;
    if (inductionVariable <= 31)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        initC2ESC($this, i, _Char___init__impl__6a9atx(117));
      }
       while (inductionVariable <= 31);
    initC2ESC($this, 8, _Char___init__impl__6a9atx(98));
    initC2ESC($this, 9, _Char___init__impl__6a9atx(116));
    initC2ESC($this, 10, _Char___init__impl__6a9atx(110));
    initC2ESC($this, 12, _Char___init__impl__6a9atx(102));
    initC2ESC($this, 13, _Char___init__impl__6a9atx(114));
    initC2ESC_0($this, _Char___init__impl__6a9atx(47), _Char___init__impl__6a9atx(47));
    initC2ESC_0($this, _Char___init__impl__6a9atx(34), _Char___init__impl__6a9atx(34));
    initC2ESC_0($this, _Char___init__impl__6a9atx(92), _Char___init__impl__6a9atx(92));
  }
  function initCharToToken($this) {
    var inductionVariable = 0;
    if (inductionVariable <= 32)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        initC2TC($this, i, 127);
      }
       while (inductionVariable <= 32);
    initC2TC($this, 9, 3);
    initC2TC($this, 10, 3);
    initC2TC($this, 13, 3);
    initC2TC($this, 32, 3);
    initC2TC_0($this, _Char___init__impl__6a9atx(44), 4);
    initC2TC_0($this, _Char___init__impl__6a9atx(58), 5);
    initC2TC_0($this, _Char___init__impl__6a9atx(123), 6);
    initC2TC_0($this, _Char___init__impl__6a9atx(125), 7);
    initC2TC_0($this, _Char___init__impl__6a9atx(91), 8);
    initC2TC_0($this, _Char___init__impl__6a9atx(93), 9);
    initC2TC_0($this, _Char___init__impl__6a9atx(34), 1);
    initC2TC_0($this, _Char___init__impl__6a9atx(92), 2);
  }
  function initC2ESC($this, c, esc) {
    if (!(esc === _Char___init__impl__6a9atx(117))) {
      // Inline function 'kotlin.code' call
      var tmp$ret$0 = Char__toInt_impl_vasixd(esc);
      $this.a1b_1[tmp$ret$0] = numberToChar(c);
    }
  }
  function initC2ESC_0($this, c, esc) {
    // Inline function 'kotlin.code' call
    var tmp$ret$0 = Char__toInt_impl_vasixd(c);
    return initC2ESC($this, tmp$ret$0, esc);
  }
  function initC2TC($this, c, cl) {
    $this.b1b_1[c] = cl;
  }
  function initC2TC_0($this, c, cl) {
    // Inline function 'kotlin.code' call
    var tmp$ret$0 = Char__toInt_impl_vasixd(c);
    return initC2TC($this, tmp$ret$0, cl);
  }
  function CharMappings() {
    CharMappings_instance = this;
    this.a1b_1 = charArray(117);
    this.b1b_1 = new Int8Array(126);
    initEscape(this);
    initCharToToken(this);
  }
  var CharMappings_instance;
  function CharMappings_getInstance() {
    if (CharMappings_instance == null)
      new CharMappings();
    return CharMappings_instance;
  }
  function StringJsonLexerWithComments(source, configuration) {
    StringJsonLexer.call(this, source, configuration);
  }
  protoOf(StringJsonLexerWithComments).f16 = function () {
    var source = this.a18();
    var cpos = this.v1a();
    if (cpos >= source.length || cpos === -1)
      return 10;
    this.a10_1 = cpos + 1 | 0;
    return charToTokenClass(charCodeAt(source, cpos));
  };
  protoOf(StringJsonLexerWithComments).b16 = function () {
    var current = this.v1a();
    if (current >= this.a18().length || current === -1)
      return false;
    return this.w1a(charCodeAt(this.a18(), current));
  };
  protoOf(StringJsonLexerWithComments).r17 = function (expected) {
    var source = this.a18();
    var current = this.v1a();
    if (current >= source.length || current === -1) {
      this.a10_1 = -1;
      this.x1a(expected);
    }
    var c = charCodeAt(source, current);
    this.a10_1 = current + 1 | 0;
    if (c === expected)
      return Unit_instance;
    else {
      this.x1a(expected);
    }
  };
  protoOf(StringJsonLexerWithComments).a16 = function () {
    var source = this.a18();
    var cpos = this.v1a();
    if (cpos >= source.length || cpos === -1)
      return 10;
    this.a10_1 = cpos;
    return charToTokenClass(charCodeAt(source, cpos));
  };
  protoOf(StringJsonLexerWithComments).v1a = function () {
    var current = this.a10_1;
    if (current === -1)
      return current;
    var source = this.a18();
    $l$loop_1: while (current < source.length) {
      var c = charCodeAt(source, current);
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.isWs' call
      if (c === _Char___init__impl__6a9atx(32) || c === _Char___init__impl__6a9atx(10) || c === _Char___init__impl__6a9atx(13) || c === _Char___init__impl__6a9atx(9)) {
        current = current + 1 | 0;
        continue $l$loop_1;
      }
      if (c === _Char___init__impl__6a9atx(47) && (current + 1 | 0) < source.length) {
        var tmp0_subject = charCodeAt(source, current + 1 | 0);
        if (tmp0_subject === _Char___init__impl__6a9atx(47)) {
          current = indexOf_0(source, _Char___init__impl__6a9atx(10), current + 2 | 0);
          if (current === -1) {
            current = source.length;
          } else {
            current = current + 1 | 0;
          }
          continue $l$loop_1;
        } else if (tmp0_subject === _Char___init__impl__6a9atx(42)) {
          current = indexOf(source, '*/', current + 2 | 0);
          if (current === -1) {
            this.a10_1 = source.length;
            this.s14('Expected end of the block comment: "*/", but had EOF instead');
          } else {
            current = current + 2 | 0;
          }
          continue $l$loop_1;
        }
      }
      break $l$loop_1;
    }
    this.a10_1 = current;
    return current;
  };
  function StringJsonLexer(source, configuration) {
    AbstractJsonLexer.call(this, configuration);
    this.n1b_1 = source;
  }
  protoOf(StringJsonLexer).a18 = function () {
    return this.n1b_1;
  };
  protoOf(StringJsonLexer).t1a = function (position) {
    return position < this.a18().length ? position : -1;
  };
  protoOf(StringJsonLexer).f16 = function () {
    var source = this.a18();
    var cpos = this.a10_1;
    $l$loop: while (!(cpos === -1) && cpos < source.length) {
      var _unary__edvuaz = cpos;
      cpos = _unary__edvuaz + 1 | 0;
      var c = charCodeAt(source, _unary__edvuaz);
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.isWs' call
      if (c === _Char___init__impl__6a9atx(32) || c === _Char___init__impl__6a9atx(10) || c === _Char___init__impl__6a9atx(13) || c === _Char___init__impl__6a9atx(9))
        continue $l$loop;
      this.a10_1 = cpos;
      return charToTokenClass(c);
    }
    this.a10_1 = source.length;
    return 10;
  };
  protoOf(StringJsonLexer).b16 = function () {
    var current = this.a10_1;
    if (current === -1)
      return false;
    var source = this.a18();
    $l$loop: while (current < source.length) {
      var c = charCodeAt(source, current);
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.isWs' call
      if (c === _Char___init__impl__6a9atx(32) || c === _Char___init__impl__6a9atx(10) || c === _Char___init__impl__6a9atx(13) || c === _Char___init__impl__6a9atx(9)) {
        current = current + 1 | 0;
        continue $l$loop;
      }
      this.a10_1 = current;
      return this.w1a(c);
    }
    this.a10_1 = current;
    return false;
  };
  protoOf(StringJsonLexer).v1a = function () {
    var current = this.a10_1;
    if (current === -1)
      return current;
    var source = this.a18();
    $l$loop: while (current < source.length) {
      var c = charCodeAt(source, current);
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.isWs' call
      if (c === _Char___init__impl__6a9atx(32) || c === _Char___init__impl__6a9atx(10) || c === _Char___init__impl__6a9atx(13) || c === _Char___init__impl__6a9atx(9)) {
        current = current + 1 | 0;
      } else {
        break $l$loop;
      }
    }
    this.a10_1 = current;
    return current;
  };
  protoOf(StringJsonLexer).r17 = function (expected) {
    if (this.a10_1 === -1) {
      this.x1a(expected);
    }
    var source = this.a18();
    var cpos = this.a10_1;
    $l$loop: while (cpos < source.length) {
      var _unary__edvuaz = cpos;
      cpos = _unary__edvuaz + 1 | 0;
      var c = charCodeAt(source, _unary__edvuaz);
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.isWs' call
      if (c === _Char___init__impl__6a9atx(32) || c === _Char___init__impl__6a9atx(10) || c === _Char___init__impl__6a9atx(13) || c === _Char___init__impl__6a9atx(9))
        continue $l$loop;
      this.a10_1 = cpos;
      if (c === expected)
        return Unit_instance;
      this.x1a(expected);
    }
    this.a10_1 = -1;
    this.x1a(expected);
  };
  protoOf(StringJsonLexer).x17 = function () {
    this.r17(_Char___init__impl__6a9atx(34));
    var current = this.a10_1;
    var closingQuote = indexOf_0(this.a18(), _Char___init__impl__6a9atx(34), current);
    if (closingQuote === -1) {
      this.d16();
      // Inline function 'kotlinx.serialization.json.internal.AbstractJsonLexer.fail' call
      var expected = tokenDescription(1);
      var position = false && this.a10_1 > 0 ? this.a10_1 - 1 | 0 : this.a10_1;
      var s = this.a10_1 === charSequenceLength(this.a18()) || position < 0 ? 'EOF' : toString_1(charSequenceGet(this.a18(), position));
      var tmp$ret$1 = 'Expected ' + expected + ", but had '" + s + "' instead";
      this.s14(tmp$ret$1, position);
    }
    var inductionVariable = current;
    if (inductionVariable < closingQuote)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (charCodeAt(this.a18(), i) === _Char___init__impl__6a9atx(92)) {
          return this.consumeString2(this.a18(), this.a10_1, i);
        }
      }
       while (inductionVariable < closingQuote);
    this.a10_1 = closingQuote + 1 | 0;
    return substring(this.a18(), current, closingQuote);
  };
  protoOf(StringJsonLexer).z17 = function (keyToMatch, isLenient) {
    var positionSnapshot = this.a10_1;
    try {
      if (!(this.f16() === 6))
        return null;
      var firstKey = this.u17(isLenient);
      if (!(firstKey === keyToMatch))
        return null;
      this.y1a();
      if (!(this.f16() === 5))
        return null;
      return this.u17(isLenient);
    }finally {
      this.a10_1 = positionSnapshot;
      this.y1a();
    }
  };
  function StringJsonLexer_0(json, source) {
    return !json.iz_1.p11_1 ? new StringJsonLexer(source, json.iz_1) : new StringJsonLexerWithComments(source, json.iz_1);
  }
  function get_schemaCache(_this__u8e3s4) {
    return _this__u8e3s4.kz_1;
  }
  function StringJsonWriter() {
    this.pz_1 = StringBuilder_init_$Create$_0(128);
  }
  protoOf(StringJsonWriter).w13 = function (value) {
    this.pz_1.m9(value);
  };
  protoOf(StringJsonWriter).q13 = function (char) {
    this.pz_1.q(char);
  };
  protoOf(StringJsonWriter).s13 = function (text) {
    this.pz_1.o(text);
  };
  protoOf(StringJsonWriter).c14 = function (text) {
    printQuoted(this.pz_1, text);
  };
  protoOf(StringJsonWriter).qz = function () {
    this.pz_1.p9();
  };
  protoOf(StringJsonWriter).toString = function () {
    return this.pz_1.toString();
  };
  function createMapForCache(initialCapacity) {
    return HashMap_init_$Create$(initialCapacity);
  }
  //region block: post-declaration
  protoOf(defer$1).pg = get_isNullable;
  protoOf(defer$1).vg = get_isInline;
  protoOf(defer$1).xg = get_annotations;
  defineProp(protoOf(JsonException), 'message', function () {
    return this.d8();
  });
  protoOf(JsonSerializersModuleValidator).bz = contextual;
  //endregion
  //region block: init
  Companion_instance = new Companion();
  Companion_instance_0 = new Companion_0();
  Companion_instance_1 = new Companion_1();
  Companion_instance_2 = new Companion_2();
  Tombstone_instance = new Tombstone();
  RedactedKey_instance = new RedactedKey();
  //endregion
  //region block: exports
  _.$_$ = _.$_$ || {};
  _.$_$.a = Default_getInstance;
  _.$_$.b = JsonObject;
  _.$_$.c = JsonPrimitive;
  _.$_$.d = Json_0;
  _.$_$.e = get_intOrNull;
  //endregion
  return _;
}));

//# sourceMappingURL=kotlinx-serialization-kotlinx-serialization-json.js.map
