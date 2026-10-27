(function (factory) {
  if (typeof define === 'function' && define.amd)
    define(['exports', './kotlin-kotlin-stdlib.js'], factory);
  else if (typeof exports === 'object')
    factory(module.exports, require('./kotlin-kotlin-stdlib.js'));
  else {
    if (typeof globalThis['kotlin-kotlin-stdlib'] === 'undefined') {
      throw new Error("Error loading module 'kotlinx-serialization-kotlinx-serialization-core'. Its dependency 'kotlin-kotlin-stdlib' was not found. Please, check whether 'kotlin-kotlin-stdlib' is loaded prior to 'kotlinx-serialization-kotlinx-serialization-core'.");
    }
    globalThis['kotlinx-serialization-kotlinx-serialization-core'] = factory(typeof globalThis['kotlinx-serialization-kotlinx-serialization-core'] === 'undefined' ? {} : globalThis['kotlinx-serialization-kotlinx-serialization-core'], globalThis['kotlin-kotlin-stdlib']);
  }
}(function (_, kotlin_kotlin) {
  'use strict';
  //region block: imports
  var imul = Math.imul;
  var protoOf = kotlin_kotlin.$_$.a8;
  var initMetadataForInterface = kotlin_kotlin.$_$.j7;
  var VOID = kotlin_kotlin.$_$.b;
  var StringCompanionObject_instance = kotlin_kotlin.$_$.b2;
  var Unit_instance = kotlin_kotlin.$_$.o2;
  var constructCallableReference = kotlin_kotlin.$_$.z6;
  var emptyList = kotlin_kotlin.$_$.u4;
  var LazyThreadSafetyMode_PUBLICATION_getInstance = kotlin_kotlin.$_$.u1;
  var lazy = kotlin_kotlin.$_$.pa;
  var KProperty1 = kotlin_kotlin.$_$.n8;
  var getPropertyCallableRef = kotlin_kotlin.$_$.d7;
  var toString = kotlin_kotlin.$_$.c8;
  var initMetadataForClass = kotlin_kotlin.$_$.g7;
  var getKClassFromExpression = kotlin_kotlin.$_$.k8;
  var IllegalArgumentException_init_$Init$ = kotlin_kotlin.$_$.h3;
  var objectCreate = kotlin_kotlin.$_$.z7;
  var captureStack = kotlin_kotlin.$_$.t6;
  var IllegalArgumentException_init_$Init$_0 = kotlin_kotlin.$_$.i3;
  var IllegalArgumentException_init_$Init$_1 = kotlin_kotlin.$_$.k3;
  var IllegalArgumentException = kotlin_kotlin.$_$.t9;
  var collectionSizeOrDefault = kotlin_kotlin.$_$.e4;
  var ArrayList_init_$Create$ = kotlin_kotlin.$_$.p2;
  var THROW_CCE = kotlin_kotlin.$_$.x9;
  var KClass = kotlin_kotlin.$_$.m8;
  var isInterface = kotlin_kotlin.$_$.t7;
  var Triple = kotlin_kotlin.$_$.y9;
  var getKClass = kotlin_kotlin.$_$.l8;
  var Pair = kotlin_kotlin.$_$.v9;
  var Entry = kotlin_kotlin.$_$.u3;
  var KtMap = kotlin_kotlin.$_$.v3;
  var KtMutableMap = kotlin_kotlin.$_$.x3;
  var LinkedHashMap = kotlin_kotlin.$_$.r3;
  var HashMap = kotlin_kotlin.$_$.p3;
  var KtSet = kotlin_kotlin.$_$.z3;
  var KtMutableSet = kotlin_kotlin.$_$.y3;
  var LinkedHashSet = kotlin_kotlin.$_$.s3;
  var HashSet = kotlin_kotlin.$_$.q3;
  var Collection = kotlin_kotlin.$_$.o3;
  var KtList = kotlin_kotlin.$_$.t3;
  var KtMutableList = kotlin_kotlin.$_$.w3;
  var ArrayList = kotlin_kotlin.$_$.n3;
  var copyToArray = kotlin_kotlin.$_$.r4;
  var _Result___get_value__impl__bjfvqg = kotlin_kotlin.$_$.j;
  var _Result___get_isFailure__impl__jpiriv = kotlin_kotlin.$_$.i;
  var Result = kotlin_kotlin.$_$.w9;
  var ensureNotNull = kotlin_kotlin.$_$.la;
  var equals = kotlin_kotlin.$_$.b7;
  var getStringHashCode = kotlin_kotlin.$_$.e7;
  var isBlank = kotlin_kotlin.$_$.t8;
  var IllegalArgumentException_init_$Create$ = kotlin_kotlin.$_$.j3;
  var toList = kotlin_kotlin.$_$.r5;
  var ArrayList_init_$Create$_0 = kotlin_kotlin.$_$.q2;
  var HashSet_init_$Create$ = kotlin_kotlin.$_$.w2;
  var toHashSet = kotlin_kotlin.$_$.p5;
  var toBooleanArray = kotlin_kotlin.$_$.o5;
  var withIndex = kotlin_kotlin.$_$.v5;
  var to = kotlin_kotlin.$_$.ua;
  var toMap = kotlin_kotlin.$_$.s5;
  var lazy_0 = kotlin_kotlin.$_$.qa;
  var contentEquals = kotlin_kotlin.$_$.f4;
  var initMetadataForObject = kotlin_kotlin.$_$.l7;
  var Long = kotlin_kotlin.$_$.u9;
  var Char = kotlin_kotlin.$_$.n9;
  var Duration__toIsoString_impl_9h6wsm = kotlin_kotlin.$_$.c;
  var Duration = kotlin_kotlin.$_$.k9;
  var Companion_getInstance = kotlin_kotlin.$_$.d2;
  var Instant = kotlin_kotlin.$_$.l9;
  var Companion_getInstance_0 = kotlin_kotlin.$_$.e2;
  var Uuid = kotlin_kotlin.$_$.m9;
  var Companion_getInstance_1 = kotlin_kotlin.$_$.f2;
  var toIntOrNull = kotlin_kotlin.$_$.b9;
  var hashCode = kotlin_kotlin.$_$.f7;
  var IllegalStateException_init_$Create$ = kotlin_kotlin.$_$.l3;
  var IndexOutOfBoundsException_init_$Create$ = kotlin_kotlin.$_$.m3;
  var ArrayList_init_$Create$_1 = kotlin_kotlin.$_$.r2;
  var HashSet_init_$Create$_0 = kotlin_kotlin.$_$.x2;
  var LinkedHashSet_init_$Create$ = kotlin_kotlin.$_$.c3;
  var LinkedHashSet_init_$Create$_0 = kotlin_kotlin.$_$.d3;
  var HashMap_init_$Create$ = kotlin_kotlin.$_$.t2;
  var HashMap_init_$Create$_0 = kotlin_kotlin.$_$.u2;
  var LinkedHashMap_init_$Create$ = kotlin_kotlin.$_$.z2;
  var LinkedHashMap_init_$Create$_0 = kotlin_kotlin.$_$.a3;
  var isArray = kotlin_kotlin.$_$.m7;
  var arrayIterator = kotlin_kotlin.$_$.r6;
  var coerceAtLeast = kotlin_kotlin.$_$.e8;
  var addAll = kotlin_kotlin.$_$.a4;
  var HashSet_init_$Create$_1 = kotlin_kotlin.$_$.v2;
  var LinkedHashSet_init_$Create$_1 = kotlin_kotlin.$_$.b3;
  var until = kotlin_kotlin.$_$.i8;
  var step = kotlin_kotlin.$_$.h8;
  var getValue = kotlin_kotlin.$_$.x4;
  var HashMap_init_$Create$_1 = kotlin_kotlin.$_$.s2;
  var LinkedHashMap_init_$Create$_1 = kotlin_kotlin.$_$.y2;
  var longArray = kotlin_kotlin.$_$.w7;
  var initMetadataForCompanion = kotlin_kotlin.$_$.h7;
  var get_lastIndex = kotlin_kotlin.$_$.c5;
  var shiftLeft = kotlin_kotlin.$_$.n6;
  var bitwiseOr = kotlin_kotlin.$_$.b6;
  var equalsLong = kotlin_kotlin.$_$.g6;
  var invert = kotlin_kotlin.$_$.i6;
  var countTrailingZeroBits = kotlin_kotlin.$_$.ja;
  var indexOf = kotlin_kotlin.$_$.y4;
  var contentToString = kotlin_kotlin.$_$.h4;
  var Enum = kotlin_kotlin.$_$.r9;
  var joinToString = kotlin_kotlin.$_$.b5;
  var toString_0 = kotlin_kotlin.$_$.ta;
  var KTypeParameter = kotlin_kotlin.$_$.o8;
  var booleanArray = kotlin_kotlin.$_$.s6;
  var emptyMap = kotlin_kotlin.$_$.v4;
  var contentHashCode = kotlin_kotlin.$_$.g4;
  var Companion_getInstance_2 = kotlin_kotlin.$_$.g2;
  var isCharArray = kotlin_kotlin.$_$.p7;
  var copyOf = kotlin_kotlin.$_$.l4;
  var DoubleCompanionObject_instance = kotlin_kotlin.$_$.x1;
  var isDoubleArray = kotlin_kotlin.$_$.q7;
  var copyOf_0 = kotlin_kotlin.$_$.n4;
  var FloatCompanionObject_instance = kotlin_kotlin.$_$.y1;
  var isFloatArray = kotlin_kotlin.$_$.r7;
  var copyOf_1 = kotlin_kotlin.$_$.o4;
  var Companion_getInstance_3 = kotlin_kotlin.$_$.i2;
  var isLongArray = kotlin_kotlin.$_$.j6;
  var copyOf_2 = kotlin_kotlin.$_$.j4;
  var Companion_getInstance_4 = kotlin_kotlin.$_$.m2;
  var _ULongArray___get_size__impl__ju6dtr = kotlin_kotlin.$_$.j1;
  var ULongArray = kotlin_kotlin.$_$.da;
  var _ULongArray___get_storage__impl__28e64j = kotlin_kotlin.$_$.k1;
  var _ULongArray___init__impl__twm1l3 = kotlin_kotlin.$_$.g1;
  var _ULong___init__impl__c78o9k = kotlin_kotlin.$_$.c1;
  var ULongArray__get_impl_pr71q9 = kotlin_kotlin.$_$.h1;
  var _ULong___get_data__impl__fggpzb = kotlin_kotlin.$_$.d1;
  var IntCompanionObject_instance = kotlin_kotlin.$_$.z1;
  var isIntArray = kotlin_kotlin.$_$.s7;
  var copyOf_3 = kotlin_kotlin.$_$.q4;
  var Companion_getInstance_5 = kotlin_kotlin.$_$.l2;
  var _UIntArray___get_size__impl__r6l8ci = kotlin_kotlin.$_$.a1;
  var UIntArray = kotlin_kotlin.$_$.ba;
  var _UIntArray___get_storage__impl__92a0v0 = kotlin_kotlin.$_$.b1;
  var _UIntArray___init__impl__ghjpc6 = kotlin_kotlin.$_$.x;
  var _UInt___init__impl__l7qpdl = kotlin_kotlin.$_$.t;
  var UIntArray__get_impl_gp5kza = kotlin_kotlin.$_$.y;
  var _UInt___get_data__impl__f0vqqw = kotlin_kotlin.$_$.u;
  var ShortCompanionObject_instance = kotlin_kotlin.$_$.a2;
  var isShortArray = kotlin_kotlin.$_$.u7;
  var copyOf_4 = kotlin_kotlin.$_$.i4;
  var Companion_getInstance_6 = kotlin_kotlin.$_$.n2;
  var _UShortArray___get_size__impl__jqto1b = kotlin_kotlin.$_$.s1;
  var UShortArray = kotlin_kotlin.$_$.fa;
  var _UShortArray___get_storage__impl__t2jpv5 = kotlin_kotlin.$_$.t1;
  var _UShortArray___init__impl__9b26ef = kotlin_kotlin.$_$.p1;
  var _UShort___init__impl__jigrne = kotlin_kotlin.$_$.l1;
  var UShortArray__get_impl_fnbhmx = kotlin_kotlin.$_$.q1;
  var _UShort___get_data__impl__g0245 = kotlin_kotlin.$_$.m1;
  var ByteCompanionObject_instance = kotlin_kotlin.$_$.w1;
  var isByteArray = kotlin_kotlin.$_$.o7;
  var copyOf_5 = kotlin_kotlin.$_$.m4;
  var Companion_getInstance_7 = kotlin_kotlin.$_$.k2;
  var _UByteArray___get_size__impl__h6pkdv = kotlin_kotlin.$_$.r;
  var UByteArray = kotlin_kotlin.$_$.z9;
  var _UByteArray___get_storage__impl__d4kctt = kotlin_kotlin.$_$.s;
  var _UByteArray___init__impl__ip4y9n = kotlin_kotlin.$_$.n;
  var _UByte___init__impl__g9hnc4 = kotlin_kotlin.$_$.k;
  var UByteArray__get_impl_t5f3hv = kotlin_kotlin.$_$.p;
  var _UByte___get_data__impl__jof9qr = kotlin_kotlin.$_$.l;
  var BooleanCompanionObject_instance = kotlin_kotlin.$_$.v1;
  var isBooleanArray = kotlin_kotlin.$_$.n7;
  var copyOf_6 = kotlin_kotlin.$_$.k4;
  var charArray = kotlin_kotlin.$_$.u6;
  var _ULongArray___init__impl__twm1l3_0 = kotlin_kotlin.$_$.f1;
  var ULongArray__set_impl_z19mvh = kotlin_kotlin.$_$.i1;
  var _UIntArray___init__impl__ghjpc6_0 = kotlin_kotlin.$_$.w;
  var UIntArray__set_impl_7f2zu2 = kotlin_kotlin.$_$.z;
  var _UShortArray___init__impl__9b26ef_0 = kotlin_kotlin.$_$.o1;
  var UShortArray__set_impl_6d8whp = kotlin_kotlin.$_$.r1;
  var _UByteArray___init__impl__ip4y9n_0 = kotlin_kotlin.$_$.o;
  var UByteArray__set_impl_jvcicn = kotlin_kotlin.$_$.q;
  var Unit = kotlin_kotlin.$_$.ha;
  var trimIndent = kotlin_kotlin.$_$.j9;
  var charSequenceLength = kotlin_kotlin.$_$.x6;
  var lastOrNull = kotlin_kotlin.$_$.e5;
  var get_lastIndex_0 = kotlin_kotlin.$_$.d5;
  var UInt = kotlin_kotlin.$_$.ca;
  var ULong = kotlin_kotlin.$_$.ea;
  var UByte = kotlin_kotlin.$_$.aa;
  var UShort = kotlin_kotlin.$_$.ga;
  var noWhenBranchMatchedException = kotlin_kotlin.$_$.ra;
  var get_indices = kotlin_kotlin.$_$.a5;
  var get_indices_0 = kotlin_kotlin.$_$.z4;
  var PrimitiveClasses_getInstance = kotlin_kotlin.$_$.c2;
  var mapOf = kotlin_kotlin.$_$.i5;
  var get_js = kotlin_kotlin.$_$.v7;
  var findAssociatedObject = kotlin_kotlin.$_$.a;
  var Companion_instance = kotlin_kotlin.$_$.j2;
  var _Result___init__impl__xyqfz8 = kotlin_kotlin.$_$.h;
  var createFailure = kotlin_kotlin.$_$.ka;
  //endregion
  //region block: pre-declaration
  initMetadataForInterface(SerializationStrategy, 'SerializationStrategy');
  initMetadataForInterface(DeserializationStrategy, 'DeserializationStrategy');
  initMetadataForInterface(KSerializer, 'KSerializer', VOID, VOID, [SerializationStrategy, DeserializationStrategy]);
  initMetadataForClass(AbstractPolymorphicSerializer, 'AbstractPolymorphicSerializer', VOID, VOID, [KSerializer]);
  initMetadataForClass(PolymorphicSerializer, 'PolymorphicSerializer', VOID, AbstractPolymorphicSerializer);
  initMetadataForClass(SerializationException, 'SerializationException', SerializationException_init_$Create$, IllegalArgumentException);
  initMetadataForClass(UnknownFieldException, 'UnknownFieldException', VOID, SerializationException);
  initMetadataForClass(MissingFieldException, 'MissingFieldException', VOID, SerializationException);
  function get_isNullable() {
    return false;
  }
  function get_isInline() {
    return false;
  }
  function get_annotations() {
    return emptyList();
  }
  initMetadataForInterface(SerialDescriptor, 'SerialDescriptor');
  initMetadataForClass(ContextDescriptor, 'ContextDescriptor', VOID, VOID, [SerialDescriptor]);
  initMetadataForClass(elementDescriptors$1);
  initMetadataForClass(elementDescriptors$$inlined$Iterable$1);
  initMetadataForClass(elementNames$1);
  initMetadataForClass(elementNames$$inlined$Iterable$1);
  initMetadataForClass(ClassSerialDescriptorBuilder, 'ClassSerialDescriptorBuilder');
  initMetadataForInterface(CachedNames, 'CachedNames');
  initMetadataForClass(SerialDescriptorImpl, 'SerialDescriptorImpl', VOID, VOID, [SerialDescriptor, CachedNames]);
  initMetadataForClass(SerialKind, 'SerialKind');
  initMetadataForObject(ENUM, 'ENUM', VOID, SerialKind);
  initMetadataForObject(CONTEXTUAL, 'CONTEXTUAL', VOID, SerialKind);
  initMetadataForClass(StructureKind, 'StructureKind', VOID, SerialKind);
  initMetadataForObject(CLASS, 'CLASS', VOID, StructureKind);
  initMetadataForObject(LIST, 'LIST', VOID, StructureKind);
  initMetadataForObject(MAP, 'MAP', VOID, StructureKind);
  initMetadataForObject(OBJECT, 'OBJECT', VOID, StructureKind);
  initMetadataForClass(PrimitiveKind, 'PrimitiveKind', VOID, SerialKind);
  initMetadataForObject(BOOLEAN, 'BOOLEAN', VOID, PrimitiveKind);
  initMetadataForObject(BYTE, 'BYTE', VOID, PrimitiveKind);
  initMetadataForObject(CHAR, 'CHAR', VOID, PrimitiveKind);
  initMetadataForObject(SHORT, 'SHORT', VOID, PrimitiveKind);
  initMetadataForObject(INT, 'INT', VOID, PrimitiveKind);
  initMetadataForObject(LONG, 'LONG', VOID, PrimitiveKind);
  initMetadataForObject(FLOAT, 'FLOAT', VOID, PrimitiveKind);
  initMetadataForObject(DOUBLE, 'DOUBLE', VOID, PrimitiveKind);
  initMetadataForObject(STRING, 'STRING', VOID, PrimitiveKind);
  initMetadataForClass(PolymorphicKind, 'PolymorphicKind', VOID, SerialKind);
  initMetadataForObject(SEALED, 'SEALED', VOID, PolymorphicKind);
  initMetadataForObject(OPEN, 'OPEN', VOID, PolymorphicKind);
  function decodeSerializableValue(deserializer) {
    return deserializer.qf(this);
  }
  initMetadataForInterface(Decoder, 'Decoder');
  function decodeSequentially() {
    return false;
  }
  function decodeCollectionSize(descriptor) {
    return -1;
  }
  function decodeSerializableElement$default(descriptor, index, deserializer, previousValue, $super) {
    previousValue = previousValue === VOID ? null : previousValue;
    return $super === VOID ? this.cj(descriptor, index, deserializer, previousValue) : $super.cj.call(this, descriptor, index, deserializer, previousValue);
  }
  initMetadataForInterface(CompositeDecoder, 'CompositeDecoder');
  initMetadataForClass(AbstractDecoder, 'AbstractDecoder', VOID, VOID, [Decoder, CompositeDecoder]);
  function encodeNotNullMark() {
  }
  function beginCollection(descriptor, collectionSize) {
    return this.qi(descriptor);
  }
  function encodeSerializableValue(serializer, value) {
    serializer.pf(this, value);
  }
  function encodeNullableSerializableValue(serializer, value) {
    var isNullabilitySupported = serializer.of().pg();
    if (isNullabilitySupported) {
      return this.ik(isInterface(serializer, SerializationStrategy) ? serializer : THROW_CCE(), value);
    }
    if (value == null) {
      this.lj();
    } else {
      this.lk();
      this.ik(serializer, value);
    }
  }
  initMetadataForInterface(Encoder, 'Encoder');
  function shouldEncodeElementDefault(descriptor, index) {
    return true;
  }
  initMetadataForInterface(CompositeEncoder, 'CompositeEncoder');
  initMetadataForClass(AbstractEncoder, 'AbstractEncoder', VOID, VOID, [Encoder, CompositeEncoder]);
  initMetadataForObject(NothingSerializer_0, 'NothingSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(DurationSerializer, 'DurationSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(InstantSerializer, 'InstantSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(UuidSerializer, 'UuidSerializer', VOID, VOID, [KSerializer]);
  initMetadataForClass(ListLikeDescriptor, 'ListLikeDescriptor', VOID, VOID, [SerialDescriptor]);
  initMetadataForClass(ArrayListClassDesc, 'ArrayListClassDesc', VOID, ListLikeDescriptor);
  initMetadataForClass(HashSetClassDesc, 'HashSetClassDesc', VOID, ListLikeDescriptor);
  initMetadataForClass(LinkedHashSetClassDesc, 'LinkedHashSetClassDesc', VOID, ListLikeDescriptor);
  initMetadataForClass(MapLikeDescriptor, 'MapLikeDescriptor', VOID, VOID, [SerialDescriptor]);
  initMetadataForClass(HashMapClassDesc, 'HashMapClassDesc', VOID, MapLikeDescriptor);
  initMetadataForClass(LinkedHashMapClassDesc, 'LinkedHashMapClassDesc', VOID, MapLikeDescriptor);
  initMetadataForClass(ArrayClassDesc, 'ArrayClassDesc', VOID, ListLikeDescriptor);
  initMetadataForClass(PrimitiveArrayDescriptor, 'PrimitiveArrayDescriptor', VOID, ListLikeDescriptor);
  initMetadataForClass(AbstractCollectionSerializer, 'AbstractCollectionSerializer', VOID, VOID, [KSerializer]);
  initMetadataForClass(CollectionLikeSerializer, 'CollectionLikeSerializer', VOID, AbstractCollectionSerializer);
  initMetadataForClass(CollectionSerializer, 'CollectionSerializer', VOID, CollectionLikeSerializer);
  initMetadataForClass(ArrayListSerializer, 'ArrayListSerializer', VOID, CollectionSerializer);
  initMetadataForClass(HashSetSerializer, 'HashSetSerializer', VOID, CollectionSerializer);
  initMetadataForClass(LinkedHashSetSerializer, 'LinkedHashSetSerializer', VOID, CollectionSerializer);
  initMetadataForClass(MapLikeSerializer, 'MapLikeSerializer', VOID, AbstractCollectionSerializer);
  initMetadataForClass(HashMapSerializer, 'HashMapSerializer', VOID, MapLikeSerializer);
  initMetadataForClass(LinkedHashMapSerializer, 'LinkedHashMapSerializer', VOID, MapLikeSerializer);
  initMetadataForClass(ReferenceArraySerializer, 'ReferenceArraySerializer', VOID, CollectionLikeSerializer);
  initMetadataForClass(PrimitiveArraySerializer, 'PrimitiveArraySerializer', VOID, CollectionLikeSerializer);
  initMetadataForClass(PrimitiveArrayBuilder, 'PrimitiveArrayBuilder');
  initMetadataForCompanion(Companion);
  initMetadataForClass(ElementMarker, 'ElementMarker');
  initMetadataForClass(EnumSerializer, 'EnumSerializer', VOID, VOID, [KSerializer]);
  initMetadataForClass(PluginGeneratedSerialDescriptor, 'PluginGeneratedSerialDescriptor', VOID, VOID, [SerialDescriptor, CachedNames]);
  initMetadataForClass(EnumDescriptor, 'EnumDescriptor', VOID, PluginGeneratedSerialDescriptor);
  initMetadataForClass(InlineClassDescriptor, 'InlineClassDescriptor', VOID, PluginGeneratedSerialDescriptor);
  function typeParametersSerializers() {
    return get_EMPTY_SERIALIZER_ARRAY();
  }
  initMetadataForInterface(GeneratedSerializer, 'GeneratedSerializer', VOID, VOID, [KSerializer]);
  initMetadataForClass(InlinePrimitiveDescriptor$1, VOID, VOID, VOID, [GeneratedSerializer]);
  initMetadataForObject(NoOpEncoder, 'NoOpEncoder', VOID, AbstractEncoder);
  initMetadataForObject(NothingSerialDescriptor, 'NothingSerialDescriptor', VOID, VOID, [SerialDescriptor]);
  initMetadataForClass(NullableSerializer, 'NullableSerializer', VOID, VOID, [KSerializer]);
  initMetadataForClass(SerialDescriptorForNullable, 'SerialDescriptorForNullable', VOID, VOID, [SerialDescriptor, CachedNames]);
  initMetadataForClass(ObjectSerializer, 'ObjectSerializer', VOID, VOID, [KSerializer]);
  initMetadataForInterface(SerializerFactory, 'SerializerFactory');
  initMetadataForObject(CharArraySerializer_0, 'CharArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForObject(DoubleArraySerializer_0, 'DoubleArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForObject(FloatArraySerializer_0, 'FloatArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForObject(LongArraySerializer_0, 'LongArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForObject(ULongArraySerializer_0, 'ULongArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForObject(IntArraySerializer_0, 'IntArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForObject(UIntArraySerializer_0, 'UIntArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForObject(ShortArraySerializer_0, 'ShortArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForObject(UShortArraySerializer_0, 'UShortArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForObject(ByteArraySerializer_0, 'ByteArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForObject(UByteArraySerializer_0, 'UByteArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForObject(BooleanArraySerializer_0, 'BooleanArraySerializer', VOID, PrimitiveArraySerializer, [KSerializer]);
  initMetadataForClass(CharArrayBuilder, 'CharArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForClass(DoubleArrayBuilder, 'DoubleArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForClass(FloatArrayBuilder, 'FloatArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForClass(LongArrayBuilder, 'LongArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForClass(ULongArrayBuilder, 'ULongArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForClass(IntArrayBuilder, 'IntArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForClass(UIntArrayBuilder, 'UIntArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForClass(ShortArrayBuilder, 'ShortArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForClass(UShortArrayBuilder, 'UShortArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForClass(ByteArrayBuilder, 'ByteArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForClass(UByteArrayBuilder, 'UByteArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForClass(BooleanArrayBuilder, 'BooleanArrayBuilder', VOID, PrimitiveArrayBuilder);
  initMetadataForObject(StringSerializer, 'StringSerializer', VOID, VOID, [KSerializer]);
  initMetadataForClass(PrimitiveSerialDescriptor_0, 'PrimitiveSerialDescriptor', VOID, VOID, [SerialDescriptor]);
  initMetadataForObject(IntSerializer, 'IntSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(LongSerializer, 'LongSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(ByteSerializer, 'ByteSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(ShortSerializer, 'ShortSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(CharSerializer, 'CharSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(DoubleSerializer, 'DoubleSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(FloatSerializer, 'FloatSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(BooleanSerializer, 'BooleanSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(UnitSerializer, 'UnitSerializer', VOID, VOID, [KSerializer]);
  initMetadataForClass(TaggedDecoder, 'TaggedDecoder', VOID, VOID, [Decoder, CompositeDecoder]);
  initMetadataForClass(NamedValueDecoder, 'NamedValueDecoder', VOID, TaggedDecoder);
  initMetadataForClass(MapEntry, 'MapEntry', VOID, VOID, [Entry]);
  initMetadataForClass(KeyValueSerializer, 'KeyValueSerializer', VOID, VOID, [KSerializer]);
  initMetadataForClass(MapEntrySerializer_0, 'MapEntrySerializer', VOID, KeyValueSerializer);
  initMetadataForClass(PairSerializer_0, 'PairSerializer', VOID, KeyValueSerializer);
  initMetadataForClass(TripleSerializer_0, 'TripleSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(UIntSerializer, 'UIntSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(ULongSerializer, 'ULongSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(UByteSerializer, 'UByteSerializer', VOID, VOID, [KSerializer]);
  initMetadataForObject(UShortSerializer, 'UShortSerializer', VOID, VOID, [KSerializer]);
  initMetadataForClass(SerializersModule, 'SerializersModule');
  initMetadataForClass(SerialModuleImpl, 'SerialModuleImpl', VOID, SerializersModule);
  initMetadataForClass(ContextualProvider, 'ContextualProvider');
  initMetadataForClass(Argless, 'Argless', VOID, ContextualProvider);
  initMetadataForClass(WithTypeArguments, 'WithTypeArguments', VOID, ContextualProvider);
  function contextual(kClass, serializer) {
    return this.zy(kClass, SerializersModuleCollector$contextual$lambda(serializer));
  }
  initMetadataForInterface(SerializersModuleCollector, 'SerializersModuleCollector');
  initMetadataForClass(SerializableWith, 'SerializableWith', VOID, VOID, VOID, VOID, 0);
  initMetadataForClass(createCache$1);
  initMetadataForClass(createParametrizedCache$1);
  //endregion
  function KSerializer() {
  }
  function SerializationStrategy() {
  }
  function DeserializationStrategy() {
  }
  function PolymorphicSerializer$descriptor$delegate$lambda$lambda(this$0) {
    return function ($this$buildSerialDescriptor) {
      $this$buildSerialDescriptor.zf('type', serializer_0(StringCompanionObject_instance).of());
      $this$buildSerialDescriptor.zf('value', buildSerialDescriptor('kotlinx.serialization.Polymorphic<' + this$0.ag_1.m() + '>', CONTEXTUAL_getInstance(), []));
      $this$buildSerialDescriptor.tf_1 = this$0.bg_1;
      return Unit_instance;
    };
  }
  function PolymorphicSerializer$descriptor$delegate$lambda(this$0) {
    return function () {
      var tmp = OPEN_getInstance();
      return withContext(buildSerialDescriptor('kotlinx.serialization.Polymorphic', tmp, [], PolymorphicSerializer$descriptor$delegate$lambda$lambda(this$0)), this$0.ag_1);
    };
  }
  function PolymorphicSerializer$_get_descriptor_$ref_8tw9if() {
    return constructCallableReference(function (p0) {
      return p0.of();
    }, 1, 0, 0);
  }
  function PolymorphicSerializer(baseClass) {
    AbstractPolymorphicSerializer.call(this);
    this.ag_1 = baseClass;
    this.bg_1 = emptyList();
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp.cg_1 = lazy(tmp_0, PolymorphicSerializer$descriptor$delegate$lambda(this));
  }
  protoOf(PolymorphicSerializer).dg = function () {
    return this.ag_1;
  };
  protoOf(PolymorphicSerializer).of = function () {
    var tmp0 = this.cg_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('descriptor', 1, tmp, PolymorphicSerializer$_get_descriptor_$ref_8tw9if(), null);
    return tmp0.y1();
  };
  protoOf(PolymorphicSerializer).toString = function () {
    return 'kotlinx.serialization.PolymorphicSerializer(baseClass: ' + toString(this.ag_1) + ')';
  };
  function findPolymorphicSerializer(_this__u8e3s4, encoder, value) {
    var tmp0_elvis_lhs = _this__u8e3s4.gg(encoder, value);
    var tmp;
    if (tmp0_elvis_lhs == null) {
      throwSubtypeNotRegistered(getKClassFromExpression(value), _this__u8e3s4.dg());
    } else {
      tmp = tmp0_elvis_lhs;
    }
    return tmp;
  }
  function findPolymorphicSerializer_0(_this__u8e3s4, decoder, klassName) {
    var tmp0_elvis_lhs = _this__u8e3s4.fg(decoder, klassName);
    var tmp;
    if (tmp0_elvis_lhs == null) {
      throwSubtypeNotRegistered_0(klassName, _this__u8e3s4.dg());
    } else {
      tmp = tmp0_elvis_lhs;
    }
    return tmp;
  }
  function SerializationException_init_$Init$($this) {
    IllegalArgumentException_init_$Init$($this);
    SerializationException.call($this);
    return $this;
  }
  function SerializationException_init_$Create$() {
    var tmp = SerializationException_init_$Init$(objectCreate(protoOf(SerializationException)));
    captureStack(tmp, SerializationException_init_$Create$);
    return tmp;
  }
  function SerializationException_init_$Init$_0(message, $this) {
    IllegalArgumentException_init_$Init$_0(message, $this);
    SerializationException.call($this);
    return $this;
  }
  function SerializationException_init_$Create$_0(message) {
    var tmp = SerializationException_init_$Init$_0(message, objectCreate(protoOf(SerializationException)));
    captureStack(tmp, SerializationException_init_$Create$_0);
    return tmp;
  }
  function SerializationException_init_$Init$_1(message, cause, $this) {
    IllegalArgumentException_init_$Init$_1(message, cause, $this);
    SerializationException.call($this);
    return $this;
  }
  function SerializationException() {
    captureStack(this, SerializationException);
  }
  function UnknownFieldException_init_$Init$(index, $this) {
    UnknownFieldException.call($this, 'An unknown field for index ' + index);
    return $this;
  }
  function UnknownFieldException_init_$Create$(index) {
    var tmp = UnknownFieldException_init_$Init$(index, objectCreate(protoOf(UnknownFieldException)));
    captureStack(tmp, UnknownFieldException_init_$Create$);
    return tmp;
  }
  function UnknownFieldException(message) {
    SerializationException_init_$Init$_0(message, this);
    captureStack(this, UnknownFieldException);
  }
  function MissingFieldException_init_$Init$(missingFields, serialName, $this) {
    var tmp0_message = missingFields.x() === 1 ? "Field '" + missingFields.y(0) + "' is required for type with serial name '" + serialName + "', but it was missing" : 'Fields ' + toString(missingFields) + " are required for type with serial name '" + serialName + "', but they were missing";
    MissingFieldException.call($this, tmp0_message, null, missingFields, serialName);
    return $this;
  }
  function MissingFieldException_init_$Create$(missingFields, serialName) {
    var tmp = MissingFieldException_init_$Init$(missingFields, serialName, objectCreate(protoOf(MissingFieldException)));
    captureStack(tmp, MissingFieldException_init_$Create$);
    return tmp;
  }
  function MissingFieldException(message, cause, missingFields, serialName) {
    SerializationException_init_$Init$_1(message, cause, this);
    captureStack(this, MissingFieldException);
    this.hg_1 = missingFields;
    this.ig_1 = serialName;
  }
  protoOf(MissingFieldException).jg = function (newMessage) {
    return new MissingFieldException(newMessage, this, this.hg_1, this.ig_1);
  };
  function serializerOrNull(_this__u8e3s4) {
    var tmp0_elvis_lhs = compiledSerializerImpl(_this__u8e3s4);
    return tmp0_elvis_lhs == null ? builtinSerializerOrNull(_this__u8e3s4) : tmp0_elvis_lhs;
  }
  function serializersForParameters(_this__u8e3s4, typeArguments, failOnMissingTypeArgSerializer) {
    var tmp;
    if (failOnMissingTypeArgSerializer) {
      // Inline function 'kotlin.collections.map' call
      // Inline function 'kotlin.collections.mapTo' call
      var destination = ArrayList_init_$Create$(collectionSizeOrDefault(typeArguments, 10));
      var _iterator__ex2g4s = typeArguments.r();
      while (_iterator__ex2g4s.s()) {
        var item = _iterator__ex2g4s.t();
        var tmp$ret$2 = serializer(_this__u8e3s4, item);
        destination.w(tmp$ret$2);
      }
      tmp = destination;
    } else {
      // Inline function 'kotlin.collections.map' call
      // Inline function 'kotlin.collections.mapTo' call
      var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(typeArguments, 10));
      var _iterator__ex2g4s_0 = typeArguments.r();
      while (_iterator__ex2g4s_0.s()) {
        var item_0 = _iterator__ex2g4s_0.t();
        var tmp0_elvis_lhs = serializerOrNull_0(_this__u8e3s4, item_0);
        var tmp_0;
        if (tmp0_elvis_lhs == null) {
          return null;
        } else {
          tmp_0 = tmp0_elvis_lhs;
        }
        var tmp$ret$5 = tmp_0;
        destination_0.w(tmp$ret$5);
      }
      tmp = destination_0;
    }
    var serializers = tmp;
    return serializers;
  }
  function parametrizedSerializerOrNull(_this__u8e3s4, serializers, elementClassifierIfArray) {
    var tmp0_elvis_lhs = builtinParametrizedSerializer(_this__u8e3s4, serializers, elementClassifierIfArray);
    return tmp0_elvis_lhs == null ? compiledParametrizedSerializer(_this__u8e3s4, serializers) : tmp0_elvis_lhs;
  }
  function serializer(_this__u8e3s4, type) {
    var tmp0_elvis_lhs = serializerByKTypeImpl(_this__u8e3s4, type, true);
    var tmp;
    if (tmp0_elvis_lhs == null) {
      platformSpecificSerializerNotRegistered(kclass(type));
    } else {
      tmp = tmp0_elvis_lhs;
    }
    return tmp;
  }
  function serializerOrNull_0(_this__u8e3s4, type) {
    return serializerByKTypeImpl(_this__u8e3s4, type, false);
  }
  function builtinParametrizedSerializer(_this__u8e3s4, serializers, elementClassifierIfArray) {
    var tmp;
    if (_this__u8e3s4.equals(getKClass(Collection)) || _this__u8e3s4.equals(getKClass(KtList)) || (_this__u8e3s4.equals(getKClass(KtMutableList)) || _this__u8e3s4.equals(getKClass(ArrayList)))) {
      tmp = new ArrayListSerializer(serializers.y(0));
    } else if (_this__u8e3s4.equals(getKClass(HashSet))) {
      tmp = new HashSetSerializer(serializers.y(0));
    } else if (_this__u8e3s4.equals(getKClass(KtSet)) || (_this__u8e3s4.equals(getKClass(KtMutableSet)) || _this__u8e3s4.equals(getKClass(LinkedHashSet)))) {
      tmp = new LinkedHashSetSerializer(serializers.y(0));
    } else if (_this__u8e3s4.equals(getKClass(HashMap))) {
      tmp = new HashMapSerializer(serializers.y(0), serializers.y(1));
    } else if (_this__u8e3s4.equals(getKClass(KtMap)) || (_this__u8e3s4.equals(getKClass(KtMutableMap)) || _this__u8e3s4.equals(getKClass(LinkedHashMap)))) {
      tmp = new LinkedHashMapSerializer(serializers.y(0), serializers.y(1));
    } else if (_this__u8e3s4.equals(getKClass(Entry))) {
      tmp = MapEntrySerializer(serializers.y(0), serializers.y(1));
    } else if (_this__u8e3s4.equals(getKClass(Pair))) {
      tmp = PairSerializer(serializers.y(0), serializers.y(1));
    } else if (_this__u8e3s4.equals(getKClass(Triple))) {
      tmp = TripleSerializer(serializers.y(0), serializers.y(1), serializers.y(2));
    } else {
      var tmp_0;
      if (isReferenceArray(_this__u8e3s4)) {
        var tmp_1 = elementClassifierIfArray();
        tmp_0 = ArraySerializer((!(tmp_1 == null) ? isInterface(tmp_1, KClass) : false) ? tmp_1 : THROW_CCE(), serializers.y(0));
      } else {
        tmp_0 = null;
      }
      tmp = tmp_0;
    }
    return tmp;
  }
  function compiledParametrizedSerializer(_this__u8e3s4, serializers) {
    // Inline function 'kotlin.collections.toTypedArray' call
    var tmp$ret$0 = copyToArray(serializers);
    return constructSerializerForGivenTypeArgs(_this__u8e3s4, tmp$ret$0.slice());
  }
  function serializerByKTypeImpl(_this__u8e3s4, type, failOnMissingTypeArgSerializer) {
    var rootClass = kclass(type);
    var isNullable = type.j();
    // Inline function 'kotlin.collections.map' call
    var this_0 = type.i();
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var tmp$ret$2 = typeOrThrow(item);
      destination.w(tmp$ret$2);
    }
    var typeArguments = destination;
    var tmp;
    if (typeArguments.p()) {
      var tmp_0;
      if (isInterface_0(rootClass) && !(_this__u8e3s4.lg(rootClass) == null)) {
        tmp_0 = null;
      } else {
        tmp_0 = findCachedSerializer(rootClass, isNullable);
      }
      tmp = tmp_0;
    } else {
      var tmp_1;
      if (_this__u8e3s4.kg()) {
        tmp_1 = null;
      } else {
        // Inline function 'kotlin.Result.getOrNull' call
        var this_1 = findParametrizedCachedSerializer(rootClass, typeArguments, isNullable);
        tmp_1 = _Result___get_isFailure__impl__jpiriv(this_1) ? null : _Result___get_value__impl__bjfvqg(this_1);
      }
      tmp = tmp_1;
    }
    var cachedSerializer = tmp;
    if (!(cachedSerializer == null))
      return cachedSerializer;
    var tmp_2;
    if (typeArguments.p()) {
      var tmp0_elvis_lhs = serializerOrNull(rootClass);
      var tmp1_elvis_lhs = tmp0_elvis_lhs == null ? _this__u8e3s4.lg(rootClass) : tmp0_elvis_lhs;
      var tmp_3;
      if (tmp1_elvis_lhs == null) {
        // Inline function 'kotlinx.serialization.polymorphicIfInterface' call
        tmp_3 = isInterface_0(rootClass) ? new PolymorphicSerializer(rootClass) : null;
      } else {
        tmp_3 = tmp1_elvis_lhs;
      }
      tmp_2 = tmp_3;
    } else {
      var tmp2_elvis_lhs = serializersForParameters(_this__u8e3s4, typeArguments, failOnMissingTypeArgSerializer);
      var tmp_4;
      if (tmp2_elvis_lhs == null) {
        return null;
      } else {
        tmp_4 = tmp2_elvis_lhs;
      }
      var serializers = tmp_4;
      var tmp3_elvis_lhs = parametrizedSerializerOrNull(rootClass, serializers, serializerByKTypeImpl$lambda(typeArguments));
      var tmp4_elvis_lhs = tmp3_elvis_lhs == null ? _this__u8e3s4.mg(rootClass, serializers) : tmp3_elvis_lhs;
      var tmp_5;
      if (tmp4_elvis_lhs == null) {
        // Inline function 'kotlinx.serialization.polymorphicIfInterface' call
        tmp_5 = isInterface_0(rootClass) ? new PolymorphicSerializer(rootClass) : null;
      } else {
        tmp_5 = tmp4_elvis_lhs;
      }
      tmp_2 = tmp_5;
    }
    var contextualSerializer = tmp_2;
    var tmp_6;
    if (contextualSerializer == null) {
      tmp_6 = null;
    } else {
      // Inline function 'kotlinx.serialization.internal.cast' call
      tmp_6 = isInterface(contextualSerializer, KSerializer) ? contextualSerializer : THROW_CCE();
    }
    var tmp6_safe_receiver = tmp_6;
    return tmp6_safe_receiver == null ? null : nullable(tmp6_safe_receiver, isNullable);
  }
  function nullable(_this__u8e3s4, shouldBeNullable) {
    if (shouldBeNullable)
      return get_nullable(_this__u8e3s4);
    return isInterface(_this__u8e3s4, KSerializer) ? _this__u8e3s4 : THROW_CCE();
  }
  function serializerByKTypeImpl$lambda($typeArguments) {
    return function () {
      return $typeArguments.y(0).h();
    };
  }
  function get_SERIALIZERS_CACHE() {
    _init_properties_SerializersCache_kt__hgwi2p();
    return SERIALIZERS_CACHE;
  }
  var SERIALIZERS_CACHE;
  function get_SERIALIZERS_CACHE_NULLABLE() {
    _init_properties_SerializersCache_kt__hgwi2p();
    return SERIALIZERS_CACHE_NULLABLE;
  }
  var SERIALIZERS_CACHE_NULLABLE;
  function get_PARAMETRIZED_SERIALIZERS_CACHE() {
    _init_properties_SerializersCache_kt__hgwi2p();
    return PARAMETRIZED_SERIALIZERS_CACHE;
  }
  var PARAMETRIZED_SERIALIZERS_CACHE;
  function get_PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE() {
    _init_properties_SerializersCache_kt__hgwi2p();
    return PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE;
  }
  var PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE;
  function findCachedSerializer(clazz, isNullable) {
    _init_properties_SerializersCache_kt__hgwi2p();
    var tmp;
    if (!isNullable) {
      var tmp0_safe_receiver = get_SERIALIZERS_CACHE().ng(clazz);
      var tmp_0;
      if (tmp0_safe_receiver == null) {
        tmp_0 = null;
      } else {
        // Inline function 'kotlinx.serialization.internal.cast' call
        tmp_0 = isInterface(tmp0_safe_receiver, KSerializer) ? tmp0_safe_receiver : THROW_CCE();
      }
      tmp = tmp_0;
    } else {
      tmp = get_SERIALIZERS_CACHE_NULLABLE().ng(clazz);
    }
    return tmp;
  }
  function findParametrizedCachedSerializer(clazz, types, isNullable) {
    _init_properties_SerializersCache_kt__hgwi2p();
    var tmp;
    if (!isNullable) {
      var tmp_0 = get_PARAMETRIZED_SERIALIZERS_CACHE().og(clazz, types);
      tmp = new Result(tmp_0) instanceof Result ? tmp_0 : THROW_CCE();
    } else {
      tmp = get_PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE().og(clazz, types);
    }
    return tmp;
  }
  function SERIALIZERS_CACHE$lambda(it) {
    _init_properties_SerializersCache_kt__hgwi2p();
    var tmp0_elvis_lhs = serializerOrNull(it);
    var tmp;
    if (tmp0_elvis_lhs == null) {
      // Inline function 'kotlinx.serialization.polymorphicIfInterface' call
      tmp = isInterface_0(it) ? new PolymorphicSerializer(it) : null;
    } else {
      tmp = tmp0_elvis_lhs;
    }
    return tmp;
  }
  function SERIALIZERS_CACHE_NULLABLE$lambda(it) {
    _init_properties_SerializersCache_kt__hgwi2p();
    var tmp0_elvis_lhs = serializerOrNull(it);
    var tmp;
    if (tmp0_elvis_lhs == null) {
      // Inline function 'kotlinx.serialization.polymorphicIfInterface' call
      tmp = isInterface_0(it) ? new PolymorphicSerializer(it) : null;
    } else {
      tmp = tmp0_elvis_lhs;
    }
    var tmp1_safe_receiver = tmp;
    var tmp2_safe_receiver = tmp1_safe_receiver == null ? null : get_nullable(tmp1_safe_receiver);
    var tmp_0;
    if (tmp2_safe_receiver == null) {
      tmp_0 = null;
    } else {
      // Inline function 'kotlinx.serialization.internal.cast' call
      tmp_0 = isInterface(tmp2_safe_receiver, KSerializer) ? tmp2_safe_receiver : THROW_CCE();
    }
    return tmp_0;
  }
  function PARAMETRIZED_SERIALIZERS_CACHE$lambda(clazz, types) {
    _init_properties_SerializersCache_kt__hgwi2p();
    var serializers = ensureNotNull(serializersForParameters(EmptySerializersModule_0(), types, true));
    return parametrizedSerializerOrNull(clazz, serializers, PARAMETRIZED_SERIALIZERS_CACHE$lambda$lambda(types));
  }
  function PARAMETRIZED_SERIALIZERS_CACHE$lambda$lambda($types) {
    return function () {
      return $types.y(0).h();
    };
  }
  function PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE$lambda(clazz, types) {
    _init_properties_SerializersCache_kt__hgwi2p();
    var serializers = ensureNotNull(serializersForParameters(EmptySerializersModule_0(), types, true));
    var tmp0_safe_receiver = parametrizedSerializerOrNull(clazz, serializers, PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE$lambda$lambda(types));
    var tmp1_safe_receiver = tmp0_safe_receiver == null ? null : get_nullable(tmp0_safe_receiver);
    var tmp;
    if (tmp1_safe_receiver == null) {
      tmp = null;
    } else {
      // Inline function 'kotlinx.serialization.internal.cast' call
      tmp = isInterface(tmp1_safe_receiver, KSerializer) ? tmp1_safe_receiver : THROW_CCE();
    }
    return tmp;
  }
  function PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE$lambda$lambda($types) {
    return function () {
      return $types.y(0).h();
    };
  }
  var properties_initialized_SerializersCache_kt_q8kf25;
  function _init_properties_SerializersCache_kt__hgwi2p() {
    if (!properties_initialized_SerializersCache_kt_q8kf25) {
      properties_initialized_SerializersCache_kt_q8kf25 = true;
      SERIALIZERS_CACHE = createCache(SERIALIZERS_CACHE$lambda);
      SERIALIZERS_CACHE_NULLABLE = createCache(SERIALIZERS_CACHE_NULLABLE$lambda);
      PARAMETRIZED_SERIALIZERS_CACHE = createParametrizedCache(PARAMETRIZED_SERIALIZERS_CACHE$lambda);
      PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE = createParametrizedCache(PARAMETRIZED_SERIALIZERS_CACHE_NULLABLE$lambda);
    }
  }
  function serializer_0(_this__u8e3s4) {
    return StringSerializer_getInstance();
  }
  function serializer_1(_this__u8e3s4) {
    return UIntSerializer_getInstance();
  }
  function serializer_2(_this__u8e3s4) {
    return ULongSerializer_getInstance();
  }
  function serializer_3(_this__u8e3s4) {
    return UByteSerializer_getInstance();
  }
  function serializer_4(_this__u8e3s4) {
    return UShortSerializer_getInstance();
  }
  function get_nullable(_this__u8e3s4) {
    var tmp;
    if (_this__u8e3s4.of().pg()) {
      tmp = isInterface(_this__u8e3s4, KSerializer) ? _this__u8e3s4 : THROW_CCE();
    } else {
      tmp = new NullableSerializer(_this__u8e3s4);
    }
    return tmp;
  }
  function serializer_5(_this__u8e3s4) {
    return IntSerializer_getInstance();
  }
  function serializer_6(_this__u8e3s4) {
    return LongSerializer_getInstance();
  }
  function serializer_7(_this__u8e3s4) {
    return ByteSerializer_getInstance();
  }
  function serializer_8(_this__u8e3s4) {
    return ShortSerializer_getInstance();
  }
  function serializer_9(_this__u8e3s4) {
    return CharSerializer_getInstance();
  }
  function CharArraySerializer() {
    return CharArraySerializer_getInstance();
  }
  function serializer_10(_this__u8e3s4) {
    return DoubleSerializer_getInstance();
  }
  function DoubleArraySerializer() {
    return DoubleArraySerializer_getInstance();
  }
  function serializer_11(_this__u8e3s4) {
    return FloatSerializer_getInstance();
  }
  function FloatArraySerializer() {
    return FloatArraySerializer_getInstance();
  }
  function LongArraySerializer() {
    return LongArraySerializer_getInstance();
  }
  function ULongArraySerializer() {
    return ULongArraySerializer_getInstance();
  }
  function IntArraySerializer() {
    return IntArraySerializer_getInstance();
  }
  function UIntArraySerializer() {
    return UIntArraySerializer_getInstance();
  }
  function ShortArraySerializer() {
    return ShortArraySerializer_getInstance();
  }
  function UShortArraySerializer() {
    return UShortArraySerializer_getInstance();
  }
  function ByteArraySerializer() {
    return ByteArraySerializer_getInstance();
  }
  function UByteArraySerializer() {
    return UByteArraySerializer_getInstance();
  }
  function serializer_12(_this__u8e3s4) {
    return BooleanSerializer_getInstance();
  }
  function BooleanArraySerializer() {
    return BooleanArraySerializer_getInstance();
  }
  function serializer_13(_this__u8e3s4) {
    return UnitSerializer_getInstance();
  }
  function NothingSerializer() {
    return NothingSerializer_getInstance();
  }
  function serializer_14(_this__u8e3s4) {
    return DurationSerializer_getInstance();
  }
  function serializer_15(_this__u8e3s4) {
    return InstantSerializer_getInstance();
  }
  function serializer_16(_this__u8e3s4) {
    return UuidSerializer_getInstance();
  }
  function MapEntrySerializer(keySerializer, valueSerializer) {
    return new MapEntrySerializer_0(keySerializer, valueSerializer);
  }
  function PairSerializer(keySerializer, valueSerializer) {
    return new PairSerializer_0(keySerializer, valueSerializer);
  }
  function TripleSerializer(aSerializer, bSerializer, cSerializer) {
    return new TripleSerializer_0(aSerializer, bSerializer, cSerializer);
  }
  function ArraySerializer(kClass, elementSerializer) {
    return new ReferenceArraySerializer(kClass, elementSerializer);
  }
  function MapSerializer(keySerializer, valueSerializer) {
    return new LinkedHashMapSerializer(keySerializer, valueSerializer);
  }
  function ListSerializer(elementSerializer) {
    return new ArrayListSerializer(elementSerializer);
  }
  function withContext(_this__u8e3s4, context) {
    return new ContextDescriptor(_this__u8e3s4, context);
  }
  function ContextDescriptor(original, kClass) {
    this.qg_1 = original;
    this.rg_1 = kClass;
    this.sg_1 = this.qg_1.tg() + '<' + this.rg_1.m() + '>';
  }
  protoOf(ContextDescriptor).tg = function () {
    return this.sg_1;
  };
  protoOf(ContextDescriptor).equals = function (other) {
    var tmp0_elvis_lhs = other instanceof ContextDescriptor ? other : null;
    var tmp;
    if (tmp0_elvis_lhs == null) {
      return false;
    } else {
      tmp = tmp0_elvis_lhs;
    }
    var another = tmp;
    return equals(this.qg_1, another.qg_1) && another.rg_1.equals(this.rg_1);
  };
  protoOf(ContextDescriptor).hashCode = function () {
    var result = this.rg_1.hashCode();
    result = imul(31, result) + getStringHashCode(this.sg_1) | 0;
    return result;
  };
  protoOf(ContextDescriptor).toString = function () {
    return 'ContextDescriptor(kClass: ' + toString(this.rg_1) + ', original: ' + toString(this.qg_1) + ')';
  };
  protoOf(ContextDescriptor).ug = function () {
    return this.qg_1.ug();
  };
  protoOf(ContextDescriptor).pg = function () {
    return this.qg_1.pg();
  };
  protoOf(ContextDescriptor).vg = function () {
    return this.qg_1.vg();
  };
  protoOf(ContextDescriptor).wg = function () {
    return this.qg_1.wg();
  };
  protoOf(ContextDescriptor).xg = function () {
    return this.qg_1.xg();
  };
  protoOf(ContextDescriptor).yg = function (index) {
    return this.qg_1.yg(index);
  };
  protoOf(ContextDescriptor).zg = function (name) {
    return this.qg_1.zg(name);
  };
  protoOf(ContextDescriptor).ah = function (index) {
    return this.qg_1.ah(index);
  };
  protoOf(ContextDescriptor).bh = function (index) {
    return this.qg_1.bh(index);
  };
  protoOf(ContextDescriptor).ch = function (index) {
    return this.qg_1.ch(index);
  };
  function getContextualDescriptor(_this__u8e3s4, descriptor) {
    var tmp0_safe_receiver = get_capturedKClass(descriptor);
    var tmp;
    if (tmp0_safe_receiver == null) {
      tmp = null;
    } else {
      // Inline function 'kotlin.let' call
      var tmp0_safe_receiver_0 = _this__u8e3s4.lg(tmp0_safe_receiver);
      tmp = tmp0_safe_receiver_0 == null ? null : tmp0_safe_receiver_0.of();
    }
    return tmp;
  }
  function get_capturedKClass(_this__u8e3s4) {
    var tmp;
    if (_this__u8e3s4 instanceof ContextDescriptor) {
      tmp = _this__u8e3s4.rg_1;
    } else {
      if (_this__u8e3s4 instanceof SerialDescriptorForNullable) {
        tmp = get_capturedKClass(_this__u8e3s4.dh_1);
      } else {
        tmp = null;
      }
    }
    return tmp;
  }
  function SerialDescriptor() {
  }
  function get_elementDescriptors(_this__u8e3s4) {
    // Inline function 'kotlin.collections.Iterable' call
    return new elementDescriptors$$inlined$Iterable$1(_this__u8e3s4);
  }
  function get_elementNames(_this__u8e3s4) {
    // Inline function 'kotlin.collections.Iterable' call
    return new elementNames$$inlined$Iterable$1(_this__u8e3s4);
  }
  function elementDescriptors$1($this_elementDescriptors) {
    this.hh_1 = $this_elementDescriptors;
    this.gh_1 = $this_elementDescriptors.wg();
  }
  protoOf(elementDescriptors$1).s = function () {
    return this.gh_1 > 0;
  };
  protoOf(elementDescriptors$1).t = function () {
    var tmp = this.hh_1.wg();
    var _unary__edvuaz = this.gh_1;
    this.gh_1 = _unary__edvuaz - 1 | 0;
    return this.hh_1.bh(tmp - _unary__edvuaz | 0);
  };
  function elementDescriptors$$inlined$Iterable$1($this_elementDescriptors) {
    this.ih_1 = $this_elementDescriptors;
  }
  protoOf(elementDescriptors$$inlined$Iterable$1).r = function () {
    return new elementDescriptors$1(this.ih_1);
  };
  function elementNames$1($this_elementNames) {
    this.kh_1 = $this_elementNames;
    this.jh_1 = $this_elementNames.wg();
  }
  protoOf(elementNames$1).s = function () {
    return this.jh_1 > 0;
  };
  protoOf(elementNames$1).t = function () {
    var tmp = this.kh_1.wg();
    var _unary__edvuaz = this.jh_1;
    this.jh_1 = _unary__edvuaz - 1 | 0;
    return this.kh_1.yg(tmp - _unary__edvuaz | 0);
  };
  function elementNames$$inlined$Iterable$1($this_elementNames) {
    this.lh_1 = $this_elementNames;
  }
  protoOf(elementNames$$inlined$Iterable$1).r = function () {
    return new elementNames$1(this.lh_1);
  };
  function buildSerialDescriptor(serialName, kind, typeParameters, builder) {
    var tmp;
    if (builder === VOID) {
      tmp = buildSerialDescriptor$lambda;
    } else {
      tmp = builder;
    }
    builder = tmp;
    // Inline function 'kotlin.text.isNotBlank' call
    // Inline function 'kotlin.require' call
    if (!!isBlank(serialName)) {
      var message = 'Blank serial names are prohibited';
      throw IllegalArgumentException_init_$Create$(toString(message));
    }
    // Inline function 'kotlin.require' call
    if (!!equals(kind, CLASS_getInstance())) {
      var message_0 = "For StructureKind.CLASS please use 'buildClassSerialDescriptor' instead";
      throw IllegalArgumentException_init_$Create$(toString(message_0));
    }
    var sdBuilder = new ClassSerialDescriptorBuilder(serialName);
    builder(sdBuilder);
    return new SerialDescriptorImpl(serialName, kind, sdBuilder.uf_1.x(), toList(typeParameters), sdBuilder);
  }
  function ClassSerialDescriptorBuilder(serialName) {
    this.rf_1 = serialName;
    this.sf_1 = false;
    this.tf_1 = emptyList();
    this.uf_1 = ArrayList_init_$Create$_0();
    this.vf_1 = HashSet_init_$Create$();
    this.wf_1 = ArrayList_init_$Create$_0();
    this.xf_1 = ArrayList_init_$Create$_0();
    this.yf_1 = ArrayList_init_$Create$_0();
  }
  protoOf(ClassSerialDescriptorBuilder).mh = function (elementName, descriptor, annotations, isOptional) {
    // Inline function 'kotlin.require' call
    if (!this.vf_1.w(elementName)) {
      var message = "Element with name '" + elementName + "' is already registered in " + this.rf_1;
      throw IllegalArgumentException_init_$Create$(toString(message));
    }
    // Inline function 'kotlin.collections.plusAssign' call
    this.uf_1.w(elementName);
    // Inline function 'kotlin.collections.plusAssign' call
    this.wf_1.w(descriptor);
    // Inline function 'kotlin.collections.plusAssign' call
    this.xf_1.w(annotations);
    // Inline function 'kotlin.collections.plusAssign' call
    this.yf_1.w(isOptional);
  };
  protoOf(ClassSerialDescriptorBuilder).zf = function (elementName, descriptor, annotations, isOptional, $super) {
    annotations = annotations === VOID ? emptyList() : annotations;
    isOptional = isOptional === VOID ? false : isOptional;
    var tmp;
    if ($super === VOID) {
      this.mh(elementName, descriptor, annotations, isOptional);
      tmp = Unit_instance;
    } else {
      tmp = $super.mh.call(this, elementName, descriptor, annotations, isOptional);
    }
    return tmp;
  };
  function _get__hashCode__tgwhef($this) {
    var tmp0 = $this.yh_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('_hashCode', 1, tmp, SerialDescriptorImpl$_get__hashCode_$ref_2v7wzp(), null);
    return tmp0.y1();
  }
  function SerialDescriptorImpl$_hashCode$delegate$lambda(this$0) {
    return function () {
      return hashCodeImpl(this$0, this$0.xh_1);
    };
  }
  function SerialDescriptorImpl$_get__hashCode_$ref_2v7wzp() {
    return constructCallableReference(function (p0) {
      return _get__hashCode__tgwhef(p0);
    }, 1, 0, 1);
  }
  function SerialDescriptorImpl(serialName, kind, elementsCount, typeParameters, builder) {
    this.nh_1 = serialName;
    this.oh_1 = kind;
    this.ph_1 = elementsCount;
    this.qh_1 = builder.tf_1;
    this.rh_1 = toHashSet(builder.uf_1);
    var tmp = this;
    // Inline function 'kotlin.collections.toTypedArray' call
    var this_0 = builder.uf_1;
    tmp.sh_1 = copyToArray(this_0);
    this.th_1 = compactArray(builder.wf_1);
    var tmp_0 = this;
    // Inline function 'kotlin.collections.toTypedArray' call
    var this_1 = builder.xf_1;
    tmp_0.uh_1 = copyToArray(this_1);
    this.vh_1 = toBooleanArray(builder.yf_1);
    var tmp_1 = this;
    // Inline function 'kotlin.collections.map' call
    var this_2 = withIndex(this.sh_1);
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_2, 10));
    var _iterator__ex2g4s = this_2.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var tmp$ret$4 = to(item.ja_1, item.ia_1);
      destination.w(tmp$ret$4);
    }
    tmp_1.wh_1 = toMap(destination);
    this.xh_1 = compactArray(typeParameters);
    var tmp_2 = this;
    tmp_2.yh_1 = lazy_0(SerialDescriptorImpl$_hashCode$delegate$lambda(this));
  }
  protoOf(SerialDescriptorImpl).tg = function () {
    return this.nh_1;
  };
  protoOf(SerialDescriptorImpl).ug = function () {
    return this.oh_1;
  };
  protoOf(SerialDescriptorImpl).wg = function () {
    return this.ph_1;
  };
  protoOf(SerialDescriptorImpl).xg = function () {
    return this.qh_1;
  };
  protoOf(SerialDescriptorImpl).zh = function () {
    return this.rh_1;
  };
  protoOf(SerialDescriptorImpl).yg = function (index) {
    return getChecked(this.sh_1, index);
  };
  protoOf(SerialDescriptorImpl).zg = function (name) {
    var tmp0_elvis_lhs = this.wh_1.b2(name);
    return tmp0_elvis_lhs == null ? -3 : tmp0_elvis_lhs;
  };
  protoOf(SerialDescriptorImpl).ah = function (index) {
    return getChecked(this.uh_1, index);
  };
  protoOf(SerialDescriptorImpl).bh = function (index) {
    return getChecked(this.th_1, index);
  };
  protoOf(SerialDescriptorImpl).ch = function (index) {
    return getChecked_0(this.vh_1, index);
  };
  protoOf(SerialDescriptorImpl).equals = function (other) {
    var tmp$ret$0;
    $l$block_5: {
      // Inline function 'kotlinx.serialization.internal.equalsImpl' call
      if (this === other) {
        tmp$ret$0 = true;
        break $l$block_5;
      }
      if (!(other instanceof SerialDescriptorImpl)) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.tg() === other.tg())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!contentEquals(this.xh_1, other.xh_1)) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.wg() === other.wg())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      var inductionVariable = 0;
      var last = this.wg();
      if (inductionVariable < last)
        do {
          var index = inductionVariable;
          inductionVariable = inductionVariable + 1 | 0;
          if (!(this.bh(index).tg() === other.bh(index).tg())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
          if (!equals(this.bh(index).ug(), other.bh(index).ug())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
        }
         while (inductionVariable < last);
      tmp$ret$0 = true;
    }
    return tmp$ret$0;
  };
  protoOf(SerialDescriptorImpl).hashCode = function () {
    return _get__hashCode__tgwhef(this);
  };
  protoOf(SerialDescriptorImpl).toString = function () {
    return toStringImpl(this);
  };
  function buildClassSerialDescriptor(serialName, typeParameters, builderAction) {
    var tmp;
    if (builderAction === VOID) {
      tmp = buildClassSerialDescriptor$lambda;
    } else {
      tmp = builderAction;
    }
    builderAction = tmp;
    // Inline function 'kotlin.text.isNotBlank' call
    // Inline function 'kotlin.require' call
    if (!!isBlank(serialName)) {
      var message = 'Blank serial names are prohibited';
      throw IllegalArgumentException_init_$Create$(toString(message));
    }
    var sdBuilder = new ClassSerialDescriptorBuilder(serialName);
    builderAction(sdBuilder);
    return new SerialDescriptorImpl(serialName, CLASS_getInstance(), sdBuilder.uf_1.x(), toList(typeParameters), sdBuilder);
  }
  function PrimitiveSerialDescriptor(serialName, kind) {
    // Inline function 'kotlin.text.isNotBlank' call
    // Inline function 'kotlin.require' call
    if (!!isBlank(serialName)) {
      var message = 'Blank serial names are prohibited';
      throw IllegalArgumentException_init_$Create$(toString(message));
    }
    return PrimitiveDescriptorSafe(serialName, kind);
  }
  function buildSerialDescriptor$lambda(_this__u8e3s4) {
    return Unit_instance;
  }
  function buildClassSerialDescriptor$lambda(_this__u8e3s4) {
    return Unit_instance;
  }
  function ENUM() {
    ENUM_instance = this;
    SerialKind.call(this);
  }
  var ENUM_instance;
  function ENUM_getInstance() {
    if (ENUM_instance == null)
      new ENUM();
    return ENUM_instance;
  }
  function CONTEXTUAL() {
    CONTEXTUAL_instance = this;
    SerialKind.call(this);
  }
  var CONTEXTUAL_instance;
  function CONTEXTUAL_getInstance() {
    if (CONTEXTUAL_instance == null)
      new CONTEXTUAL();
    return CONTEXTUAL_instance;
  }
  function SerialKind() {
  }
  protoOf(SerialKind).toString = function () {
    return ensureNotNull(getKClassFromExpression(this).m());
  };
  protoOf(SerialKind).hashCode = function () {
    return getStringHashCode(this.toString());
  };
  function CLASS() {
    CLASS_instance = this;
    StructureKind.call(this);
  }
  var CLASS_instance;
  function CLASS_getInstance() {
    if (CLASS_instance == null)
      new CLASS();
    return CLASS_instance;
  }
  function LIST() {
    LIST_instance = this;
    StructureKind.call(this);
  }
  var LIST_instance;
  function LIST_getInstance() {
    if (LIST_instance == null)
      new LIST();
    return LIST_instance;
  }
  function MAP() {
    MAP_instance = this;
    StructureKind.call(this);
  }
  var MAP_instance;
  function MAP_getInstance() {
    if (MAP_instance == null)
      new MAP();
    return MAP_instance;
  }
  function OBJECT() {
    OBJECT_instance = this;
    StructureKind.call(this);
  }
  var OBJECT_instance;
  function OBJECT_getInstance() {
    if (OBJECT_instance == null)
      new OBJECT();
    return OBJECT_instance;
  }
  function StructureKind() {
    SerialKind.call(this);
  }
  function BOOLEAN() {
    BOOLEAN_instance = this;
    PrimitiveKind.call(this);
  }
  var BOOLEAN_instance;
  function BOOLEAN_getInstance() {
    if (BOOLEAN_instance == null)
      new BOOLEAN();
    return BOOLEAN_instance;
  }
  function BYTE() {
    BYTE_instance = this;
    PrimitiveKind.call(this);
  }
  var BYTE_instance;
  function BYTE_getInstance() {
    if (BYTE_instance == null)
      new BYTE();
    return BYTE_instance;
  }
  function CHAR() {
    CHAR_instance = this;
    PrimitiveKind.call(this);
  }
  var CHAR_instance;
  function CHAR_getInstance() {
    if (CHAR_instance == null)
      new CHAR();
    return CHAR_instance;
  }
  function SHORT() {
    SHORT_instance = this;
    PrimitiveKind.call(this);
  }
  var SHORT_instance;
  function SHORT_getInstance() {
    if (SHORT_instance == null)
      new SHORT();
    return SHORT_instance;
  }
  function INT() {
    INT_instance = this;
    PrimitiveKind.call(this);
  }
  var INT_instance;
  function INT_getInstance() {
    if (INT_instance == null)
      new INT();
    return INT_instance;
  }
  function LONG() {
    LONG_instance = this;
    PrimitiveKind.call(this);
  }
  var LONG_instance;
  function LONG_getInstance() {
    if (LONG_instance == null)
      new LONG();
    return LONG_instance;
  }
  function FLOAT() {
    FLOAT_instance = this;
    PrimitiveKind.call(this);
  }
  var FLOAT_instance;
  function FLOAT_getInstance() {
    if (FLOAT_instance == null)
      new FLOAT();
    return FLOAT_instance;
  }
  function DOUBLE() {
    DOUBLE_instance = this;
    PrimitiveKind.call(this);
  }
  var DOUBLE_instance;
  function DOUBLE_getInstance() {
    if (DOUBLE_instance == null)
      new DOUBLE();
    return DOUBLE_instance;
  }
  function STRING() {
    STRING_instance = this;
    PrimitiveKind.call(this);
  }
  var STRING_instance;
  function STRING_getInstance() {
    if (STRING_instance == null)
      new STRING();
    return STRING_instance;
  }
  function PrimitiveKind() {
    SerialKind.call(this);
  }
  function SEALED() {
    SEALED_instance = this;
    PolymorphicKind.call(this);
  }
  var SEALED_instance;
  function SEALED_getInstance() {
    if (SEALED_instance == null)
      new SEALED();
    return SEALED_instance;
  }
  function OPEN() {
    OPEN_instance = this;
    PolymorphicKind.call(this);
  }
  var OPEN_instance;
  function OPEN_getInstance() {
    if (OPEN_instance == null)
      new OPEN();
    return OPEN_instance;
  }
  function PolymorphicKind() {
    SerialKind.call(this);
  }
  function AbstractDecoder() {
  }
  protoOf(AbstractDecoder).ai = function () {
    throw SerializationException_init_$Create$_0(toString(getKClassFromExpression(this)) + " can't retrieve untyped values");
  };
  protoOf(AbstractDecoder).bi = function () {
    return true;
  };
  protoOf(AbstractDecoder).ci = function () {
    return null;
  };
  protoOf(AbstractDecoder).di = function () {
    var tmp = this.ai();
    return typeof tmp === 'boolean' ? tmp : THROW_CCE();
  };
  protoOf(AbstractDecoder).ei = function () {
    var tmp = this.ai();
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(AbstractDecoder).fi = function () {
    var tmp = this.ai();
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(AbstractDecoder).gi = function () {
    var tmp = this.ai();
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(AbstractDecoder).hi = function () {
    var tmp = this.ai();
    return tmp instanceof Long ? tmp : THROW_CCE();
  };
  protoOf(AbstractDecoder).ii = function () {
    var tmp = this.ai();
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(AbstractDecoder).ji = function () {
    var tmp = this.ai();
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(AbstractDecoder).ki = function () {
    var tmp = this.ai();
    return tmp instanceof Char ? tmp.j1_1 : THROW_CCE();
  };
  protoOf(AbstractDecoder).li = function () {
    var tmp = this.ai();
    return typeof tmp === 'string' ? tmp : THROW_CCE();
  };
  protoOf(AbstractDecoder).mi = function (enumDescriptor) {
    var tmp = this.ai();
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(AbstractDecoder).ni = function (descriptor) {
    return this;
  };
  protoOf(AbstractDecoder).oi = function (deserializer, previousValue) {
    return this.pi(deserializer);
  };
  protoOf(AbstractDecoder).qi = function (descriptor) {
    return this;
  };
  protoOf(AbstractDecoder).ri = function (descriptor) {
  };
  protoOf(AbstractDecoder).si = function (descriptor, index) {
    return this.di();
  };
  protoOf(AbstractDecoder).ti = function (descriptor, index) {
    return this.ei();
  };
  protoOf(AbstractDecoder).ui = function (descriptor, index) {
    return this.fi();
  };
  protoOf(AbstractDecoder).vi = function (descriptor, index) {
    return this.gi();
  };
  protoOf(AbstractDecoder).wi = function (descriptor, index) {
    return this.hi();
  };
  protoOf(AbstractDecoder).xi = function (descriptor, index) {
    return this.ii();
  };
  protoOf(AbstractDecoder).yi = function (descriptor, index) {
    return this.ji();
  };
  protoOf(AbstractDecoder).zi = function (descriptor, index) {
    return this.ki();
  };
  protoOf(AbstractDecoder).aj = function (descriptor, index) {
    return this.li();
  };
  protoOf(AbstractDecoder).bj = function (descriptor, index) {
    return this.ni(descriptor.bh(index));
  };
  protoOf(AbstractDecoder).cj = function (descriptor, index, deserializer, previousValue) {
    return this.oi(deserializer, previousValue);
  };
  protoOf(AbstractDecoder).ej = function (descriptor, index, deserializer, previousValue) {
    // Inline function 'kotlinx.serialization.encoding.decodeIfNullable' call
    var isNullabilitySupported = deserializer.of().pg();
    var tmp;
    if (isNullabilitySupported || this.bi()) {
      tmp = this.oi(deserializer, previousValue);
    } else {
      tmp = this.ci();
    }
    return tmp;
  };
  function AbstractEncoder() {
  }
  protoOf(AbstractEncoder).qi = function (descriptor) {
    return this;
  };
  protoOf(AbstractEncoder).ri = function (descriptor) {
  };
  protoOf(AbstractEncoder).jj = function (descriptor, index) {
    return true;
  };
  protoOf(AbstractEncoder).kj = function (value) {
    throw SerializationException_init_$Create$_0('Non-serializable ' + toString(getKClassFromExpression(value)) + ' is not supported by ' + toString(getKClassFromExpression(this)) + ' encoder');
  };
  protoOf(AbstractEncoder).lj = function () {
    throw SerializationException_init_$Create$_0("'null' is not supported by default");
  };
  protoOf(AbstractEncoder).mj = function (value) {
    return this.kj(value);
  };
  protoOf(AbstractEncoder).nj = function (value) {
    return this.kj(value);
  };
  protoOf(AbstractEncoder).oj = function (value) {
    return this.kj(value);
  };
  protoOf(AbstractEncoder).pj = function (value) {
    return this.kj(value);
  };
  protoOf(AbstractEncoder).qj = function (value) {
    return this.kj(value);
  };
  protoOf(AbstractEncoder).rj = function (value) {
    return this.kj(value);
  };
  protoOf(AbstractEncoder).sj = function (value) {
    return this.kj(value);
  };
  protoOf(AbstractEncoder).tj = function (value) {
    return this.kj(new Char(value));
  };
  protoOf(AbstractEncoder).uj = function (value) {
    return this.kj(value);
  };
  protoOf(AbstractEncoder).vj = function (enumDescriptor, index) {
    return this.kj(index);
  };
  protoOf(AbstractEncoder).wj = function (descriptor) {
    return this;
  };
  protoOf(AbstractEncoder).xj = function (descriptor, index, value) {
    if (this.jj(descriptor, index)) {
      this.mj(value);
    }
  };
  protoOf(AbstractEncoder).yj = function (descriptor, index, value) {
    if (this.jj(descriptor, index)) {
      this.nj(value);
    }
  };
  protoOf(AbstractEncoder).zj = function (descriptor, index, value) {
    if (this.jj(descriptor, index)) {
      this.oj(value);
    }
  };
  protoOf(AbstractEncoder).ak = function (descriptor, index, value) {
    if (this.jj(descriptor, index)) {
      this.pj(value);
    }
  };
  protoOf(AbstractEncoder).bk = function (descriptor, index, value) {
    if (this.jj(descriptor, index)) {
      this.qj(value);
    }
  };
  protoOf(AbstractEncoder).ck = function (descriptor, index, value) {
    if (this.jj(descriptor, index)) {
      this.rj(value);
    }
  };
  protoOf(AbstractEncoder).dk = function (descriptor, index, value) {
    if (this.jj(descriptor, index)) {
      this.sj(value);
    }
  };
  protoOf(AbstractEncoder).ek = function (descriptor, index, value) {
    if (this.jj(descriptor, index)) {
      this.tj(value);
    }
  };
  protoOf(AbstractEncoder).fk = function (descriptor, index, value) {
    if (this.jj(descriptor, index)) {
      this.uj(value);
    }
  };
  protoOf(AbstractEncoder).gk = function (descriptor, index) {
    return this.jj(descriptor, index) ? this.wj(descriptor.bh(index)) : NoOpEncoder_getInstance();
  };
  protoOf(AbstractEncoder).hk = function (descriptor, index, serializer, value) {
    if (this.jj(descriptor, index)) {
      this.ik(serializer, value);
    }
  };
  protoOf(AbstractEncoder).jk = function (descriptor, index, serializer, value) {
    if (this.jj(descriptor, index)) {
      this.kk(serializer, value);
    }
  };
  function Decoder() {
  }
  function CompositeDecoder() {
  }
  function Encoder() {
  }
  function CompositeEncoder() {
  }
  function decodeSequentially_0($this, compositeDecoder) {
    var klassName = compositeDecoder.aj($this.of(), 0);
    var serializer = findPolymorphicSerializer_0($this, compositeDecoder, klassName);
    return compositeDecoder.dj($this.of(), 1, serializer);
  }
  function AbstractPolymorphicSerializer() {
  }
  protoOf(AbstractPolymorphicSerializer).eg = function (encoder, value) {
    var actualSerializer = findPolymorphicSerializer(this, encoder, value);
    // Inline function 'kotlinx.serialization.encoding.encodeStructure' call
    var descriptor = this.of();
    var composite = encoder.qi(descriptor);
    composite.fk(this.of(), 0, actualSerializer.of().tg());
    var tmp = this.of();
    // Inline function 'kotlinx.serialization.internal.cast' call
    var tmp$ret$2 = isInterface(actualSerializer, SerializationStrategy) ? actualSerializer : THROW_CCE();
    composite.hk(tmp, 1, tmp$ret$2, value);
    composite.ri(descriptor);
  };
  protoOf(AbstractPolymorphicSerializer).pf = function (encoder, value) {
    return this.eg(encoder, !(value == null) ? value : THROW_CCE());
  };
  protoOf(AbstractPolymorphicSerializer).qf = function (decoder) {
    // Inline function 'kotlinx.serialization.encoding.decodeStructure' call
    var descriptor = this.of();
    var composite = decoder.qi(descriptor);
    var tmp$ret$1;
    $l$block: {
      var klassName = null;
      var value = null;
      if (composite.gj()) {
        tmp$ret$1 = decodeSequentially_0(this, composite);
        break $l$block;
      }
      mainLoop: while (true) {
        var index = composite.hj(this.of());
        switch (index) {
          case -1:
            break mainLoop;
          case 0:
            klassName = composite.aj(this.of(), index);
            break;
          case 1:
            var tmp0 = klassName;
            var tmp$ret$2;
            $l$block_0: {
              // Inline function 'kotlin.requireNotNull' call
              if (tmp0 == null) {
                var message = 'Cannot read polymorphic value before its type token';
                throw IllegalArgumentException_init_$Create$(toString(message));
              } else {
                tmp$ret$2 = tmp0;
                break $l$block_0;
              }
            }

            klassName = tmp$ret$2;
            var serializer = findPolymorphicSerializer_0(this, composite, klassName);
            value = composite.dj(this.of(), index, serializer);
            break;
          default:
            var tmp0_elvis_lhs = klassName;
            throw SerializationException_init_$Create$_0('Invalid index in polymorphic deserialization of ' + (tmp0_elvis_lhs == null ? 'unknown class' : tmp0_elvis_lhs) + ('\n Expected 0, 1 or DECODE_DONE(-1), but found ' + index));
        }
      }
      var tmp0_0 = value;
      var tmp$ret$4;
      $l$block_1: {
        // Inline function 'kotlin.requireNotNull' call
        if (tmp0_0 == null) {
          var message_0 = 'Polymorphic value has not been read for class ' + klassName;
          throw IllegalArgumentException_init_$Create$(toString(message_0));
        } else {
          tmp$ret$4 = tmp0_0;
          break $l$block_1;
        }
      }
      var tmp = tmp$ret$4;
      tmp$ret$1 = !(tmp == null) ? tmp : THROW_CCE();
    }
    var result = tmp$ret$1;
    composite.ri(descriptor);
    return result;
  };
  protoOf(AbstractPolymorphicSerializer).fg = function (decoder, klassName) {
    return decoder.fj().ok(this.dg(), klassName);
  };
  protoOf(AbstractPolymorphicSerializer).gg = function (encoder, value) {
    return encoder.fj().pk(this.dg(), value);
  };
  function throwSubtypeNotRegistered(subClass, baseClass) {
    var tmp0_elvis_lhs = subClass.m();
    throwSubtypeNotRegistered_0(tmp0_elvis_lhs == null ? toString(subClass) : tmp0_elvis_lhs, baseClass);
  }
  function throwSubtypeNotRegistered_0(subClassName, baseClass) {
    var scope = "in the polymorphic scope of '" + baseClass.m() + "'";
    throw SerializationException_init_$Create$_0(subClassName == null ? 'Class discriminator was missing and no default serializers were registered ' + scope + '.' : "Serializer for subclass '" + subClassName + "' is not found " + scope + '.\n' + ("Check if class with serial name '" + subClassName + "' exists and serializer is registered in a corresponding SerializersModule.\n") + ("To be registered automatically, class '" + subClassName + "' has to be '@Serializable', and the base class '" + baseClass.m() + "' has to be sealed and '@Serializable'."));
  }
  function NothingSerializer_0() {
    NothingSerializer_instance = this;
    this.qk_1 = NothingSerialDescriptor_getInstance();
  }
  protoOf(NothingSerializer_0).of = function () {
    return this.qk_1;
  };
  protoOf(NothingSerializer_0).rk = function (encoder, value) {
    throw SerializationException_init_$Create$_0("'kotlin.Nothing' cannot be serialized");
  };
  protoOf(NothingSerializer_0).pf = function (encoder, value) {
    var tmp;
    if (false) {
      tmp = value;
    } else {
      tmp = THROW_CCE();
    }
    return this.rk(encoder, tmp);
  };
  protoOf(NothingSerializer_0).qf = function (decoder) {
    throw SerializationException_init_$Create$_0("'kotlin.Nothing' does not have instances");
  };
  var NothingSerializer_instance;
  function NothingSerializer_getInstance() {
    if (NothingSerializer_instance == null)
      new NothingSerializer_0();
    return NothingSerializer_instance;
  }
  function DurationSerializer() {
    DurationSerializer_instance = this;
    this.sk_1 = new PrimitiveSerialDescriptor_0('kotlin.time.Duration', STRING_getInstance());
  }
  protoOf(DurationSerializer).of = function () {
    return this.sk_1;
  };
  protoOf(DurationSerializer).tk = function (encoder, value) {
    encoder.uj(Duration__toIsoString_impl_9h6wsm(value));
  };
  protoOf(DurationSerializer).pf = function (encoder, value) {
    return this.tk(encoder, value instanceof Duration ? value.bc_1 : THROW_CCE());
  };
  protoOf(DurationSerializer).uk = function (decoder) {
    return Companion_getInstance().cc(decoder.li());
  };
  protoOf(DurationSerializer).qf = function (decoder) {
    return new Duration(this.uk(decoder));
  };
  var DurationSerializer_instance;
  function DurationSerializer_getInstance() {
    if (DurationSerializer_instance == null)
      new DurationSerializer();
    return DurationSerializer_instance;
  }
  function InstantSerializer() {
    InstantSerializer_instance = this;
    this.vk_1 = new PrimitiveSerialDescriptor_0('kotlin.time.Instant', STRING_getInstance());
  }
  protoOf(InstantSerializer).of = function () {
    return this.vk_1;
  };
  protoOf(InstantSerializer).wk = function (encoder, value) {
    encoder.uj(value.toString());
  };
  protoOf(InstantSerializer).pf = function (encoder, value) {
    return this.wk(encoder, value instanceof Instant ? value : THROW_CCE());
  };
  protoOf(InstantSerializer).qf = function (decoder) {
    return Companion_getInstance_0().oc(decoder.li());
  };
  var InstantSerializer_instance;
  function InstantSerializer_getInstance() {
    if (InstantSerializer_instance == null)
      new InstantSerializer();
    return InstantSerializer_instance;
  }
  function UuidSerializer() {
    UuidSerializer_instance = this;
    this.xk_1 = new PrimitiveSerialDescriptor_0('kotlin.uuid.Uuid', STRING_getInstance());
  }
  protoOf(UuidSerializer).of = function () {
    return this.xk_1;
  };
  protoOf(UuidSerializer).yk = function (encoder, value) {
    encoder.uj(value.toString());
  };
  protoOf(UuidSerializer).pf = function (encoder, value) {
    return this.yk(encoder, value instanceof Uuid ? value : THROW_CCE());
  };
  protoOf(UuidSerializer).qf = function (decoder) {
    return Companion_getInstance_1().vd(decoder.li());
  };
  var UuidSerializer_instance;
  function UuidSerializer_getInstance() {
    if (UuidSerializer_instance == null)
      new UuidSerializer();
    return UuidSerializer_instance;
  }
  function CachedNames() {
  }
  function ArrayListClassDesc(elementDesc) {
    ListLikeDescriptor.call(this, elementDesc);
  }
  protoOf(ArrayListClassDesc).tg = function () {
    return 'kotlin.collections.ArrayList';
  };
  function HashSetClassDesc(elementDesc) {
    ListLikeDescriptor.call(this, elementDesc);
  }
  protoOf(HashSetClassDesc).tg = function () {
    return 'kotlin.collections.HashSet';
  };
  function LinkedHashSetClassDesc(elementDesc) {
    ListLikeDescriptor.call(this, elementDesc);
  }
  protoOf(LinkedHashSetClassDesc).tg = function () {
    return 'kotlin.collections.LinkedHashSet';
  };
  function HashMapClassDesc(keyDesc, valueDesc) {
    MapLikeDescriptor.call(this, 'kotlin.collections.HashMap', keyDesc, valueDesc);
  }
  function LinkedHashMapClassDesc(keyDesc, valueDesc) {
    MapLikeDescriptor.call(this, 'kotlin.collections.LinkedHashMap', keyDesc, valueDesc);
  }
  function ArrayClassDesc(elementDesc) {
    ListLikeDescriptor.call(this, elementDesc);
  }
  protoOf(ArrayClassDesc).tg = function () {
    return 'kotlin.Array';
  };
  function ListLikeDescriptor(elementDescriptor) {
    this.bl_1 = elementDescriptor;
    this.cl_1 = 1;
  }
  protoOf(ListLikeDescriptor).ug = function () {
    return LIST_getInstance();
  };
  protoOf(ListLikeDescriptor).wg = function () {
    return this.cl_1;
  };
  protoOf(ListLikeDescriptor).yg = function (index) {
    requireNonNegativeIndex(index, this.tg());
    return index.toString();
  };
  protoOf(ListLikeDescriptor).zg = function (name) {
    var tmp0_safe_receiver = toIntOrNull(name);
    var tmp;
    if (tmp0_safe_receiver == null) {
      tmp = null;
    } else {
      // Inline function 'kotlin.takeIf' call
      var tmp_0;
      if (tmp0_safe_receiver >= 0) {
        tmp_0 = tmp0_safe_receiver;
      } else {
        tmp_0 = null;
      }
      tmp = tmp_0;
    }
    var tmp1_elvis_lhs = tmp;
    return tmp1_elvis_lhs == null ? -3 : tmp1_elvis_lhs;
  };
  protoOf(ListLikeDescriptor).ch = function (index) {
    requireNonNegativeIndex(index, this.tg());
    return false;
  };
  protoOf(ListLikeDescriptor).ah = function (index) {
    requireNonNegativeIndex(index, this.tg());
    return emptyList();
  };
  protoOf(ListLikeDescriptor).bh = function (index) {
    requireNonNegativeIndex(index, this.tg());
    return this.bl_1;
  };
  protoOf(ListLikeDescriptor).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ListLikeDescriptor))
      return false;
    if (equals(this.bl_1, other.bl_1) && this.tg() === other.tg())
      return true;
    return false;
  };
  protoOf(ListLikeDescriptor).hashCode = function () {
    return imul(hashCode(this.bl_1), 31) + getStringHashCode(this.tg()) | 0;
  };
  protoOf(ListLikeDescriptor).toString = function () {
    return this.tg() + '(' + toString(this.bl_1) + ')';
  };
  function MapLikeDescriptor(serialName, keyDescriptor, valueDescriptor) {
    this.hl_1 = serialName;
    this.il_1 = keyDescriptor;
    this.jl_1 = valueDescriptor;
    this.kl_1 = 2;
  }
  protoOf(MapLikeDescriptor).tg = function () {
    return this.hl_1;
  };
  protoOf(MapLikeDescriptor).ug = function () {
    return MAP_getInstance();
  };
  protoOf(MapLikeDescriptor).wg = function () {
    return this.kl_1;
  };
  protoOf(MapLikeDescriptor).yg = function (index) {
    requireNonNegativeIndex(index, this.tg());
    return index.toString();
  };
  protoOf(MapLikeDescriptor).zg = function (name) {
    var tmp0_safe_receiver = toIntOrNull(name);
    var tmp;
    if (tmp0_safe_receiver == null) {
      tmp = null;
    } else {
      // Inline function 'kotlin.takeIf' call
      var tmp_0;
      if (tmp0_safe_receiver >= 0) {
        tmp_0 = tmp0_safe_receiver;
      } else {
        tmp_0 = null;
      }
      tmp = tmp_0;
    }
    var tmp1_elvis_lhs = tmp;
    return tmp1_elvis_lhs == null ? -3 : tmp1_elvis_lhs;
  };
  protoOf(MapLikeDescriptor).ch = function (index) {
    requireNonNegativeIndex(index, this.tg());
    return false;
  };
  protoOf(MapLikeDescriptor).ah = function (index) {
    requireNonNegativeIndex(index, this.tg());
    return emptyList();
  };
  protoOf(MapLikeDescriptor).bh = function (index) {
    requireNonNegativeIndex(index, this.tg());
    var tmp;
    switch (index % 2 | 0) {
      case 0:
        tmp = this.il_1;
        break;
      case 1:
        tmp = this.jl_1;
        break;
      default:
        // Inline function 'kotlin.error' call

        var message = 'Unreached';
        throw IllegalStateException_init_$Create$(toString(message));
    }
    return tmp;
  };
  protoOf(MapLikeDescriptor).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof MapLikeDescriptor))
      return false;
    if (!(this.tg() === other.tg()))
      return false;
    if (!equals(this.il_1, other.il_1))
      return false;
    if (!equals(this.jl_1, other.jl_1))
      return false;
    return true;
  };
  protoOf(MapLikeDescriptor).hashCode = function () {
    var result = getStringHashCode(this.tg());
    result = imul(31, result) + hashCode(this.il_1) | 0;
    result = imul(31, result) + hashCode(this.jl_1) | 0;
    return result;
  };
  protoOf(MapLikeDescriptor).toString = function () {
    return this.tg() + '(' + toString(this.il_1) + ', ' + toString(this.jl_1) + ')';
  };
  function requireNonNegativeIndex(index, name) {
    if (index < 0)
      throw IndexOutOfBoundsException_init_$Create$('Illegal index ' + index + ', ' + name + ' expects only non-negative indices');
  }
  function PrimitiveArrayDescriptor(primitive) {
    ListLikeDescriptor.call(this, primitive);
    this.pl_1 = primitive.tg() + 'Array';
  }
  protoOf(PrimitiveArrayDescriptor).tg = function () {
    return this.pl_1;
  };
  function ArrayListSerializer(element) {
    CollectionSerializer.call(this, element);
    this.rl_1 = new ArrayListClassDesc(element.of());
  }
  protoOf(ArrayListSerializer).of = function () {
    return this.rl_1;
  };
  protoOf(ArrayListSerializer).sl = function (initialCapacityHint) {
    if (initialCapacityHint === -1) {
      // Inline function 'kotlin.collections.arrayListOf' call
      return ArrayList_init_$Create$_0();
    }
    return ArrayList_init_$Create$(initialCapacityHint);
  };
  protoOf(ArrayListSerializer).tl = function (_this__u8e3s4) {
    return _this__u8e3s4.x();
  };
  protoOf(ArrayListSerializer).ul = function (_this__u8e3s4) {
    return this.tl(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(ArrayListSerializer).vl = function (_this__u8e3s4) {
    return _this__u8e3s4;
  };
  protoOf(ArrayListSerializer).wl = function (_this__u8e3s4) {
    return this.vl(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(ArrayListSerializer).xl = function (_this__u8e3s4, expectedSize) {
    if (_this__u8e3s4 instanceof ArrayList) {
      // Inline function 'kotlin.also' call
      _this__u8e3s4.g4(expectedSize);
      return _this__u8e3s4;
    }
    if (expectedSize === _this__u8e3s4.x())
      return ArrayList_init_$Create$_1(_this__u8e3s4);
    // Inline function 'kotlin.also' call
    var this_0 = ArrayList_init_$Create$(expectedSize);
    this_0.c1(_this__u8e3s4);
    return this_0;
  };
  protoOf(ArrayListSerializer).yl = function (_this__u8e3s4, expectedSize) {
    return this.xl((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtList) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(ArrayListSerializer).zl = function (_this__u8e3s4, index, element) {
    _this__u8e3s4.n3(index, element);
  };
  protoOf(ArrayListSerializer).am = function (_this__u8e3s4, index, element) {
    return this.zl(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE(), index, element);
  };
  function HashSetSerializer(eSerializer) {
    CollectionSerializer.call(this, eSerializer);
    this.lm_1 = new HashSetClassDesc(eSerializer.of());
  }
  protoOf(HashSetSerializer).of = function () {
    return this.lm_1;
  };
  protoOf(HashSetSerializer).sl = function (initialCapacityHint) {
    if (initialCapacityHint === -1)
      return HashSet_init_$Create$();
    return newHashSet(initialCapacityHint);
  };
  protoOf(HashSetSerializer).mm = function (_this__u8e3s4) {
    return _this__u8e3s4.x();
  };
  protoOf(HashSetSerializer).ul = function (_this__u8e3s4) {
    return this.mm(_this__u8e3s4 instanceof HashSet ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(HashSetSerializer).nm = function (_this__u8e3s4) {
    return _this__u8e3s4;
  };
  protoOf(HashSetSerializer).wl = function (_this__u8e3s4) {
    return this.nm(_this__u8e3s4 instanceof HashSet ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(HashSetSerializer).om = function (_this__u8e3s4, expectedSize) {
    if (_this__u8e3s4 instanceof HashSet)
      return _this__u8e3s4;
    if (expectedSize <= _this__u8e3s4.x())
      return HashSet_init_$Create$_0(_this__u8e3s4);
    // Inline function 'kotlin.also' call
    var this_0 = newHashSet(expectedSize);
    this_0.c1(_this__u8e3s4);
    return this_0;
  };
  protoOf(HashSetSerializer).yl = function (_this__u8e3s4, expectedSize) {
    return this.om((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtSet) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(HashSetSerializer).pm = function (_this__u8e3s4, index, element) {
    _this__u8e3s4.w(element);
  };
  protoOf(HashSetSerializer).am = function (_this__u8e3s4, index, element) {
    return this.pm(_this__u8e3s4 instanceof HashSet ? _this__u8e3s4 : THROW_CCE(), index, element);
  };
  function LinkedHashSetSerializer(eSerializer) {
    CollectionSerializer.call(this, eSerializer);
    this.rm_1 = new LinkedHashSetClassDesc(eSerializer.of());
  }
  protoOf(LinkedHashSetSerializer).of = function () {
    return this.rm_1;
  };
  protoOf(LinkedHashSetSerializer).sl = function (initialCapacityHint) {
    if (initialCapacityHint === -1) {
      // Inline function 'kotlin.collections.linkedSetOf' call
      return LinkedHashSet_init_$Create$();
    }
    return newLinkedHashSet(initialCapacityHint);
  };
  protoOf(LinkedHashSetSerializer).sm = function (_this__u8e3s4) {
    return _this__u8e3s4.x();
  };
  protoOf(LinkedHashSetSerializer).ul = function (_this__u8e3s4) {
    return this.sm(_this__u8e3s4 instanceof LinkedHashSet ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(LinkedHashSetSerializer).tm = function (_this__u8e3s4) {
    return _this__u8e3s4;
  };
  protoOf(LinkedHashSetSerializer).wl = function (_this__u8e3s4) {
    return this.tm(_this__u8e3s4 instanceof LinkedHashSet ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(LinkedHashSetSerializer).om = function (_this__u8e3s4, expectedSize) {
    if (_this__u8e3s4 instanceof LinkedHashSet)
      return _this__u8e3s4;
    if (expectedSize <= _this__u8e3s4.x())
      return LinkedHashSet_init_$Create$_0(_this__u8e3s4);
    // Inline function 'kotlin.also' call
    var this_0 = newLinkedHashSet(expectedSize);
    this_0.c1(_this__u8e3s4);
    return this_0;
  };
  protoOf(LinkedHashSetSerializer).yl = function (_this__u8e3s4, expectedSize) {
    return this.om((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtSet) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(LinkedHashSetSerializer).um = function (_this__u8e3s4, index, element) {
    _this__u8e3s4.w(element);
  };
  protoOf(LinkedHashSetSerializer).am = function (_this__u8e3s4, index, element) {
    return this.um(_this__u8e3s4 instanceof LinkedHashSet ? _this__u8e3s4 : THROW_CCE(), index, element);
  };
  function HashMapSerializer(kSerializer, vSerializer) {
    MapLikeSerializer.call(this, kSerializer, vSerializer);
    this.xm_1 = new HashMapClassDesc(kSerializer.of(), vSerializer.of());
  }
  protoOf(HashMapSerializer).of = function () {
    return this.xm_1;
  };
  protoOf(HashMapSerializer).ym = function (_this__u8e3s4) {
    return _this__u8e3s4.x();
  };
  protoOf(HashMapSerializer).zm = function (_this__u8e3s4) {
    return this.ym((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtMap) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(HashMapSerializer).an = function (_this__u8e3s4) {
    // Inline function 'kotlin.collections.iterator' call
    return _this__u8e3s4.e2().r();
  };
  protoOf(HashMapSerializer).bn = function (_this__u8e3s4) {
    return this.an((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtMap) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(HashMapSerializer).sl = function (initialCapacityHint) {
    if (initialCapacityHint === -1)
      return HashMap_init_$Create$();
    return newHashMap(initialCapacityHint);
  };
  protoOf(HashMapSerializer).cn = function (_this__u8e3s4) {
    return imul(_this__u8e3s4.x(), 2);
  };
  protoOf(HashMapSerializer).ul = function (_this__u8e3s4) {
    return this.cn(_this__u8e3s4 instanceof HashMap ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(HashMapSerializer).dn = function (_this__u8e3s4) {
    return _this__u8e3s4;
  };
  protoOf(HashMapSerializer).wl = function (_this__u8e3s4) {
    return this.dn(_this__u8e3s4 instanceof HashMap ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(HashMapSerializer).en = function (_this__u8e3s4, expectedSize) {
    if (_this__u8e3s4 instanceof HashMap)
      return _this__u8e3s4;
    if (expectedSize <= _this__u8e3s4.x())
      return HashMap_init_$Create$_0(_this__u8e3s4);
    // Inline function 'kotlin.also' call
    var this_0 = newHashMap(expectedSize);
    this_0.i2(_this__u8e3s4);
    return this_0;
  };
  protoOf(HashMapSerializer).yl = function (_this__u8e3s4, expectedSize) {
    return this.en((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtMap) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  function LinkedHashMapSerializer(kSerializer, vSerializer) {
    MapLikeSerializer.call(this, kSerializer, vSerializer);
    this.ln_1 = new LinkedHashMapClassDesc(kSerializer.of(), vSerializer.of());
  }
  protoOf(LinkedHashMapSerializer).of = function () {
    return this.ln_1;
  };
  protoOf(LinkedHashMapSerializer).ym = function (_this__u8e3s4) {
    return _this__u8e3s4.x();
  };
  protoOf(LinkedHashMapSerializer).zm = function (_this__u8e3s4) {
    return this.ym((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtMap) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(LinkedHashMapSerializer).an = function (_this__u8e3s4) {
    // Inline function 'kotlin.collections.iterator' call
    return _this__u8e3s4.e2().r();
  };
  protoOf(LinkedHashMapSerializer).bn = function (_this__u8e3s4) {
    return this.an((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtMap) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(LinkedHashMapSerializer).sl = function (initialCapacityHint) {
    if (initialCapacityHint === -1)
      return LinkedHashMap_init_$Create$();
    return newLinkedHashMap(initialCapacityHint);
  };
  protoOf(LinkedHashMapSerializer).mn = function (_this__u8e3s4) {
    return imul(_this__u8e3s4.x(), 2);
  };
  protoOf(LinkedHashMapSerializer).ul = function (_this__u8e3s4) {
    return this.mn(_this__u8e3s4 instanceof LinkedHashMap ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(LinkedHashMapSerializer).nn = function (_this__u8e3s4) {
    return _this__u8e3s4;
  };
  protoOf(LinkedHashMapSerializer).wl = function (_this__u8e3s4) {
    return this.nn(_this__u8e3s4 instanceof LinkedHashMap ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(LinkedHashMapSerializer).en = function (_this__u8e3s4, expectedSize) {
    if (_this__u8e3s4 instanceof LinkedHashMap)
      return _this__u8e3s4;
    if (expectedSize <= _this__u8e3s4.x())
      return LinkedHashMap_init_$Create$_0(_this__u8e3s4);
    // Inline function 'kotlin.also' call
    var this_0 = newLinkedHashMap(expectedSize);
    this_0.i2(_this__u8e3s4);
    return this_0;
  };
  protoOf(LinkedHashMapSerializer).yl = function (_this__u8e3s4, expectedSize) {
    return this.en((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, KtMap) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  function ReferenceArraySerializer(kClass, eSerializer) {
    CollectionLikeSerializer.call(this, eSerializer);
    this.pn_1 = kClass;
    this.qn_1 = new ArrayClassDesc(eSerializer.of());
  }
  protoOf(ReferenceArraySerializer).of = function () {
    return this.qn_1;
  };
  protoOf(ReferenceArraySerializer).rn = function (_this__u8e3s4) {
    return _this__u8e3s4.length;
  };
  protoOf(ReferenceArraySerializer).zm = function (_this__u8e3s4) {
    return this.rn((!(_this__u8e3s4 == null) ? isArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(ReferenceArraySerializer).sn = function (_this__u8e3s4) {
    return arrayIterator(_this__u8e3s4);
  };
  protoOf(ReferenceArraySerializer).bn = function (_this__u8e3s4) {
    return this.sn((!(_this__u8e3s4 == null) ? isArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(ReferenceArraySerializer).sl = function (initialCapacityHint) {
    if (initialCapacityHint === -1) {
      // Inline function 'kotlin.collections.arrayListOf' call
      return ArrayList_init_$Create$_0();
    }
    return ArrayList_init_$Create$(initialCapacityHint);
  };
  protoOf(ReferenceArraySerializer).tn = function (_this__u8e3s4) {
    return _this__u8e3s4.x();
  };
  protoOf(ReferenceArraySerializer).ul = function (_this__u8e3s4) {
    return this.tn(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(ReferenceArraySerializer).un = function (_this__u8e3s4) {
    return toNativeArrayImpl(_this__u8e3s4, this.pn_1);
  };
  protoOf(ReferenceArraySerializer).wl = function (_this__u8e3s4) {
    return this.un(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(ReferenceArraySerializer).vn = function (_this__u8e3s4, expectedSize) {
    var expectedCapacity = coerceAtLeast(expectedSize, 0);
    // Inline function 'kotlin.also' call
    var this_0 = ArrayList_init_$Create$(expectedCapacity);
    addAll(this_0, _this__u8e3s4);
    return this_0;
  };
  protoOf(ReferenceArraySerializer).yl = function (_this__u8e3s4, expectedSize) {
    return this.vn((!(_this__u8e3s4 == null) ? isArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(ReferenceArraySerializer).wn = function (_this__u8e3s4, index, element) {
    _this__u8e3s4.n3(index, element);
  };
  protoOf(ReferenceArraySerializer).am = function (_this__u8e3s4, index, element) {
    return this.wn(_this__u8e3s4 instanceof ArrayList ? _this__u8e3s4 : THROW_CCE(), index, element);
  };
  function CollectionSerializer(element) {
    CollectionLikeSerializer.call(this, element);
  }
  protoOf(CollectionSerializer).cm = function (_this__u8e3s4) {
    return _this__u8e3s4.x();
  };
  protoOf(CollectionSerializer).zm = function (_this__u8e3s4) {
    return this.cm((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, Collection) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(CollectionSerializer).dm = function (_this__u8e3s4) {
    return _this__u8e3s4.r();
  };
  protoOf(CollectionSerializer).bn = function (_this__u8e3s4) {
    return this.dm((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, Collection) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  function newHashSet(expectedCapacity) {
    var capacity = estimateCapacityForHashMap(expectedCapacity);
    return HashSet_init_$Create$_1(capacity);
  }
  function newLinkedHashSet(expectedCapacity) {
    var capacity = estimateCapacityForHashMap(expectedCapacity);
    return LinkedHashSet_init_$Create$_1(capacity);
  }
  function MapLikeSerializer(keySerializer, valueSerializer) {
    AbstractCollectionSerializer.call(this);
    this.fn_1 = keySerializer;
    this.gn_1 = valueSerializer;
  }
  protoOf(MapLikeSerializer).hn = function (decoder, builder, startIndex, size) {
    // Inline function 'kotlin.require' call
    if (!(size >= 0)) {
      var message = 'Size must be known in advance when using READ_ALL';
      throw IllegalArgumentException_init_$Create$(toString(message));
    }
    var progression = step(until(0, imul(size, 2)), 2);
    var inductionVariable = progression.f1_1;
    var last = progression.g1_1;
    var step_0 = progression.h1_1;
    if (step_0 > 0 && inductionVariable <= last || (step_0 < 0 && last <= inductionVariable))
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + step_0 | 0;
        this.in(decoder, startIndex + index | 0, builder, false);
      }
       while (!(index === last));
  };
  protoOf(MapLikeSerializer).gm = function (decoder, builder, startIndex, size) {
    return this.hn(decoder, (!(builder == null) ? isInterface(builder, KtMutableMap) : false) ? builder : THROW_CCE(), startIndex, size);
  };
  protoOf(MapLikeSerializer).in = function (decoder, index, builder, checkIndex) {
    var key = decoder.dj(this.of(), index, this.fn_1);
    var tmp;
    if (checkIndex) {
      // Inline function 'kotlin.also' call
      var this_0 = decoder.hj(this.of());
      // Inline function 'kotlin.require' call
      if (!(this_0 === (index + 1 | 0))) {
        var message = 'Value must follow key in a map, index for key: ' + index + ', returned index for value: ' + this_0;
        throw IllegalArgumentException_init_$Create$(toString(message));
      }
      tmp = this_0;
    } else {
      tmp = index + 1 | 0;
    }
    var vIndex = tmp;
    var tmp_0;
    var tmp_1;
    if (builder.z1(key)) {
      var tmp_2 = this.gn_1.of().ug();
      tmp_1 = !(tmp_2 instanceof PrimitiveKind);
    } else {
      tmp_1 = false;
    }
    if (tmp_1) {
      tmp_0 = decoder.cj(this.of(), vIndex, this.gn_1, getValue(builder, key));
    } else {
      tmp_0 = decoder.dj(this.of(), vIndex, this.gn_1);
    }
    var value = tmp_0;
    // Inline function 'kotlin.collections.set' call
    builder.h2(key, value);
  };
  protoOf(MapLikeSerializer).hm = function (decoder, index, builder, checkIndex) {
    return this.in(decoder, index, (!(builder == null) ? isInterface(builder, KtMutableMap) : false) ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(MapLikeSerializer).fm = function (encoder, value) {
    var size = this.zm(value);
    // Inline function 'kotlinx.serialization.encoding.encodeCollection' call
    var descriptor = this.of();
    var composite = encoder.mk(descriptor, size);
    var iterator = this.bn(value);
    var index = 0;
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s = iterator;
    while (_iterator__ex2g4s.s()) {
      var element = _iterator__ex2g4s.t();
      // Inline function 'kotlin.collections.component1' call
      var k = element.x1();
      // Inline function 'kotlin.collections.component2' call
      var v = element.y1();
      var tmp = this.of();
      var _unary__edvuaz = index;
      index = _unary__edvuaz + 1 | 0;
      composite.hk(tmp, _unary__edvuaz, this.fn_1, k);
      var tmp_0 = this.of();
      var _unary__edvuaz_0 = index;
      index = _unary__edvuaz_0 + 1 | 0;
      composite.hk(tmp_0, _unary__edvuaz_0, this.gn_1, v);
    }
    composite.ri(descriptor);
  };
  protoOf(MapLikeSerializer).pf = function (encoder, value) {
    return this.fm(encoder, value);
  };
  function newHashMap(expectedCapacity) {
    var capacity = estimateCapacityForHashMap(expectedCapacity);
    return HashMap_init_$Create$_1(capacity);
  }
  function newLinkedHashMap(expectedCapacity) {
    var capacity = estimateCapacityForHashMap(expectedCapacity);
    return LinkedHashMap_init_$Create$_1(capacity);
  }
  function CollectionLikeSerializer(elementSerializer) {
    AbstractCollectionSerializer.call(this);
    this.em_1 = elementSerializer;
  }
  protoOf(CollectionLikeSerializer).fm = function (encoder, value) {
    var size = this.zm(value);
    // Inline function 'kotlinx.serialization.encoding.encodeCollection' call
    var descriptor = this.of();
    var composite = encoder.mk(descriptor, size);
    var iterator = this.bn(value);
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        composite.hk(this.of(), index, this.em_1, iterator.t());
      }
       while (inductionVariable < size);
    composite.ri(descriptor);
  };
  protoOf(CollectionLikeSerializer).pf = function (encoder, value) {
    return this.fm(encoder, value);
  };
  protoOf(CollectionLikeSerializer).gm = function (decoder, builder, startIndex, size) {
    // Inline function 'kotlin.require' call
    if (!(size >= 0)) {
      var message = 'Size must be known in advance when using READ_ALL';
      throw IllegalArgumentException_init_$Create$(toString(message));
    }
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var index = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        this.hm(decoder, startIndex + index | 0, builder, false);
      }
       while (inductionVariable < size);
  };
  protoOf(CollectionLikeSerializer).hm = function (decoder, index, builder, checkIndex) {
    this.am(builder, index, decoder.dj(this.of(), index, this.em_1));
  };
  function AbstractCollectionSerializer() {
  }
  protoOf(AbstractCollectionSerializer).jm = function (decoder, previous) {
    var compositeDecoder = decoder.qi(this.of());
    var expectedSize = compositeDecoder.ij(this.of());
    // Inline function 'kotlin.require' call
    if (!(expectedSize >= -1)) {
      var message = 'Decoded collection size should be either non-negative, or -1 (meaning, unknown). Was: ' + expectedSize;
      throw IllegalArgumentException_init_$Create$(toString(message));
    }
    var tmp;
    if (!(previous == null)) {
      tmp = this.yl(previous, this.zm(previous) + coerceAtLeast(expectedSize, 0) | 0);
    } else {
      tmp = this.sl(expectedSize);
    }
    var builder = tmp;
    var startIndex = this.ul(builder);
    if (compositeDecoder.gj()) {
      this.gm(compositeDecoder, builder, startIndex, expectedSize);
    } else {
      $l$loop: while (true) {
        var index = compositeDecoder.hj(this.of());
        if (index === -1)
          break $l$loop;
        this.im(compositeDecoder, startIndex + index | 0, builder);
      }
    }
    compositeDecoder.ri(this.of());
    return this.wl(builder);
  };
  protoOf(AbstractCollectionSerializer).qf = function (decoder) {
    return this.jm(decoder, null);
  };
  protoOf(AbstractCollectionSerializer).im = function (decoder, index, builder, checkIndex, $super) {
    checkIndex = checkIndex === VOID ? true : checkIndex;
    var tmp;
    if ($super === VOID) {
      this.hm(decoder, index, builder, checkIndex);
      tmp = Unit_instance;
    } else {
      tmp = $super.hm.call(this, decoder, index, builder, checkIndex);
    }
    return tmp;
  };
  function PrimitiveArraySerializer(primitiveSerializer) {
    CollectionLikeSerializer.call(this, primitiveSerializer);
    this.yn_1 = new PrimitiveArrayDescriptor(primitiveSerializer.of());
  }
  protoOf(PrimitiveArraySerializer).of = function () {
    return this.yn_1;
  };
  protoOf(PrimitiveArraySerializer).zn = function (_this__u8e3s4) {
    return _this__u8e3s4.ao();
  };
  protoOf(PrimitiveArraySerializer).ul = function (_this__u8e3s4) {
    return this.zn(_this__u8e3s4 instanceof PrimitiveArrayBuilder ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(PrimitiveArraySerializer).bo = function (_this__u8e3s4) {
    return _this__u8e3s4.co();
  };
  protoOf(PrimitiveArraySerializer).wl = function (_this__u8e3s4) {
    return this.bo(_this__u8e3s4 instanceof PrimitiveArrayBuilder ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(PrimitiveArraySerializer).do = function (_this__u8e3s4) {
    // Inline function 'kotlin.error' call
    var message = 'This method lead to boxing and must not be used, use writeContents instead';
    throw IllegalStateException_init_$Create$(toString(message));
  };
  protoOf(PrimitiveArraySerializer).bn = function (_this__u8e3s4) {
    return this.do(_this__u8e3s4);
  };
  protoOf(PrimitiveArraySerializer).eo = function (_this__u8e3s4, index, element) {
    // Inline function 'kotlin.error' call
    var message = 'This method lead to boxing and must not be used, use Builder.append instead';
    throw IllegalStateException_init_$Create$(toString(message));
  };
  protoOf(PrimitiveArraySerializer).am = function (_this__u8e3s4, index, element) {
    return this.eo(_this__u8e3s4 instanceof PrimitiveArrayBuilder ? _this__u8e3s4 : THROW_CCE(), index, element);
  };
  protoOf(PrimitiveArraySerializer).sl = function (initialCapacityHint) {
    if (initialCapacityHint === -1)
      return this.fo();
    return this.go(initialCapacityHint);
  };
  protoOf(PrimitiveArraySerializer).jo = function (encoder, value) {
    var size = this.zm(value);
    // Inline function 'kotlinx.serialization.encoding.encodeCollection' call
    var descriptor = this.yn_1;
    var composite = encoder.mk(descriptor, size);
    this.io(composite, value, size);
    composite.ri(descriptor);
  };
  protoOf(PrimitiveArraySerializer).pf = function (encoder, value) {
    return this.jo(encoder, value);
  };
  protoOf(PrimitiveArraySerializer).fm = function (encoder, value) {
    return this.jo(encoder, value);
  };
  protoOf(PrimitiveArraySerializer).qf = function (decoder) {
    return this.jm(decoder, null);
  };
  function PrimitiveArrayBuilder() {
  }
  protoOf(PrimitiveArrayBuilder).lo = function (requiredCapacity, $super) {
    requiredCapacity = requiredCapacity === VOID ? this.ao() + 1 | 0 : requiredCapacity;
    var tmp;
    if ($super === VOID) {
      this.ko(requiredCapacity);
      tmp = Unit_instance;
    } else {
      tmp = $super.ko.call(this, requiredCapacity);
    }
    return tmp;
  };
  function Companion() {
    Companion_instance_0 = this;
    this.mo_1 = longArray(0);
  }
  var Companion_instance_0;
  function Companion_getInstance_8() {
    if (Companion_instance_0 == null)
      new Companion();
    return Companion_instance_0;
  }
  function prepareHighMarksArray($this, elementsCount) {
    var slotsCount = (elementsCount - 1 | 0) >>> 6 | 0;
    var elementsInLastSlot = elementsCount & 63;
    var highMarks = longArray(slotsCount);
    if (!(elementsInLastSlot === 0)) {
      highMarks[get_lastIndex(highMarks)] = shiftLeft(new Long(-1, -1), elementsCount);
    }
    return highMarks;
  }
  function markHigh($this, index) {
    var slot = (index >>> 6 | 0) - 1 | 0;
    var offsetInSlot = index & 63;
    $this.qo_1[slot] = bitwiseOr($this.qo_1[slot], shiftLeft(new Long(1, 0), offsetInSlot));
  }
  function nextUnmarkedHighIndex($this) {
    var inductionVariable = 0;
    var last = $this.qo_1.length - 1 | 0;
    if (inductionVariable <= last)
      do {
        var slot = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        var slotOffset = imul(slot + 1 | 0, 64);
        var slotMarks = $this.qo_1[slot];
        while (!equalsLong(slotMarks, new Long(-1, -1))) {
          var indexInSlot = countTrailingZeroBits(invert(slotMarks));
          slotMarks = bitwiseOr(slotMarks, shiftLeft(new Long(1, 0), indexInSlot));
          var index = slotOffset + indexInSlot | 0;
          if ($this.oo_1($this.no_1, index)) {
            $this.qo_1[slot] = slotMarks;
            return index;
          }
        }
        $this.qo_1[slot] = slotMarks;
      }
       while (inductionVariable <= last);
    return -1;
  }
  function ElementMarker(descriptor, readIfAbsent) {
    Companion_getInstance_8();
    this.no_1 = descriptor;
    this.oo_1 = readIfAbsent;
    var elementsCount = this.no_1.wg();
    if (elementsCount <= 64) {
      var tmp = this;
      var tmp_0;
      if (elementsCount === 64) {
        tmp_0 = new Long(0, 0);
      } else {
        tmp_0 = shiftLeft(new Long(-1, -1), elementsCount);
      }
      tmp.po_1 = tmp_0;
      this.qo_1 = Companion_getInstance_8().mo_1;
    } else {
      this.po_1 = new Long(0, 0);
      this.qo_1 = prepareHighMarksArray(this, elementsCount);
    }
  }
  protoOf(ElementMarker).ro = function (index) {
    if (index < 64) {
      this.po_1 = bitwiseOr(this.po_1, shiftLeft(new Long(1, 0), index));
    } else {
      markHigh(this, index);
    }
  };
  protoOf(ElementMarker).so = function () {
    var elementsCount = this.no_1.wg();
    while (!equalsLong(this.po_1, new Long(-1, -1))) {
      var index = countTrailingZeroBits(invert(this.po_1));
      this.po_1 = bitwiseOr(this.po_1, shiftLeft(new Long(1, 0), index));
      if (this.oo_1(this.no_1, index)) {
        return index;
      }
    }
    if (elementsCount > 64) {
      return nextUnmarkedHighIndex(this);
    }
    return -1;
  };
  function createSimpleEnumSerializer(serialName, values) {
    return new EnumSerializer(serialName, values);
  }
  function createUnmarkedDescriptor($this, serialName) {
    var d = new EnumDescriptor(serialName, $this.to_1.length);
    // Inline function 'kotlin.collections.forEach' call
    var indexedObject = $this.to_1;
    var inductionVariable = 0;
    var last = indexedObject.length;
    while (inductionVariable < last) {
      var element = indexedObject[inductionVariable];
      inductionVariable = inductionVariable + 1 | 0;
      d.ip(element.j2_1);
    }
    return d;
  }
  function EnumSerializer$descriptor$delegate$lambda(this$0, $serialName) {
    return function () {
      var tmp0_elvis_lhs = this$0.uo_1;
      return tmp0_elvis_lhs == null ? createUnmarkedDescriptor(this$0, $serialName) : tmp0_elvis_lhs;
    };
  }
  function EnumSerializer$_get_descriptor_$ref_j67dlw() {
    return constructCallableReference(function (p0) {
      return p0.of();
    }, 1, 0, 2);
  }
  function EnumSerializer(serialName, values) {
    this.to_1 = values;
    this.uo_1 = null;
    var tmp = this;
    tmp.vo_1 = lazy_0(EnumSerializer$descriptor$delegate$lambda(this, serialName));
  }
  protoOf(EnumSerializer).of = function () {
    var tmp0 = this.vo_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('descriptor', 1, tmp, EnumSerializer$_get_descriptor_$ref_j67dlw(), null);
    return tmp0.y1();
  };
  protoOf(EnumSerializer).jp = function (encoder, value) {
    var index = indexOf(this.to_1, value);
    if (index === -1) {
      throw SerializationException_init_$Create$_0(toString(value) + ' is not a valid enum ' + this.of().tg() + ', ' + ('must be one of ' + contentToString(this.to_1)));
    }
    encoder.vj(this.of(), index);
  };
  protoOf(EnumSerializer).pf = function (encoder, value) {
    return this.jp(encoder, value instanceof Enum ? value : THROW_CCE());
  };
  protoOf(EnumSerializer).qf = function (decoder) {
    var index = decoder.mi(this.of());
    if (!(0 <= index ? index <= (this.to_1.length - 1 | 0) : false)) {
      throw SerializationException_init_$Create$_0('' + index + ' is not among valid ' + this.of().tg() + ' enum values, ' + ('values size is ' + this.to_1.length));
    }
    return this.to_1[index];
  };
  protoOf(EnumSerializer).toString = function () {
    return 'kotlinx.serialization.internal.EnumSerializer<' + this.of().tg() + '>';
  };
  function _get_elementDescriptors__y23q9p($this) {
    var tmp0 = $this.xp_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('elementDescriptors', 1, tmp, EnumDescriptor$_get_elementDescriptors_$ref_5lvk4a(), null);
    return tmp0.y1();
  }
  function EnumDescriptor$elementDescriptors$delegate$lambda($elementsCount, $name, this$0) {
    return function () {
      var tmp = 0;
      var tmp_0 = $elementsCount;
      // Inline function 'kotlin.arrayOfNulls' call
      var tmp_1 = Array(tmp_0);
      while (tmp < tmp_0) {
        var tmp_2 = tmp;
        tmp_1[tmp_2] = buildSerialDescriptor($name + '.' + this$0.yg(tmp_2), OBJECT_getInstance(), []);
        tmp = tmp + 1 | 0;
      }
      return tmp_1;
    };
  }
  function EnumDescriptor$_get_elementDescriptors_$ref_5lvk4a() {
    return constructCallableReference(function (p0) {
      return _get_elementDescriptors__y23q9p(p0);
    }, 1, 0, 3);
  }
  function EnumDescriptor(name, elementsCount) {
    PluginGeneratedSerialDescriptor.call(this, name, VOID, elementsCount);
    this.wp_1 = ENUM_getInstance();
    var tmp = this;
    tmp.xp_1 = lazy_0(EnumDescriptor$elementDescriptors$delegate$lambda(elementsCount, name, this));
  }
  protoOf(EnumDescriptor).ug = function () {
    return this.wp_1;
  };
  protoOf(EnumDescriptor).bh = function (index) {
    return getChecked(_get_elementDescriptors__y23q9p(this), index);
  };
  protoOf(EnumDescriptor).equals = function (other) {
    if (this === other)
      return true;
    if (other == null)
      return false;
    if (!isInterface(other, SerialDescriptor))
      return false;
    if (!(other.ug() === ENUM_getInstance()))
      return false;
    if (!(this.tg() === other.tg()))
      return false;
    if (!equals(cachedSerialNames(this), cachedSerialNames(other)))
      return false;
    return true;
  };
  protoOf(EnumDescriptor).toString = function () {
    return joinToString(get_elementNames(this), ', ', this.tg() + '(', ')');
  };
  protoOf(EnumDescriptor).hashCode = function () {
    var result = getStringHashCode(this.tg());
    // Inline function 'kotlinx.serialization.internal.elementsHashCodeBy' call
    // Inline function 'kotlin.collections.fold' call
    var accumulator = 1;
    var _iterator__ex2g4s = get_elementNames(this).r();
    while (_iterator__ex2g4s.s()) {
      var element = _iterator__ex2g4s.t();
      var hash = accumulator;
      var tmp = imul(31, hash);
      // Inline function 'kotlin.hashCode' call
      var tmp1_elvis_lhs = element == null ? null : hashCode(element);
      accumulator = tmp + (tmp1_elvis_lhs == null ? 0 : tmp1_elvis_lhs) | 0;
    }
    var elementsHashCode = accumulator;
    result = imul(31, result) + elementsHashCode | 0;
    return result;
  };
  function InlinePrimitiveDescriptor(name, primitiveSerializer) {
    return new InlineClassDescriptor(name, new InlinePrimitiveDescriptor$1(primitiveSerializer));
  }
  function InlineClassDescriptor(name, generatedSerializer) {
    PluginGeneratedSerialDescriptor.call(this, name, generatedSerializer, 1);
    this.mq_1 = true;
  }
  protoOf(InlineClassDescriptor).vg = function () {
    return this.mq_1;
  };
  protoOf(InlineClassDescriptor).hashCode = function () {
    return imul(protoOf(PluginGeneratedSerialDescriptor).hashCode.call(this), 31);
  };
  protoOf(InlineClassDescriptor).equals = function (other) {
    var tmp$ret$0;
    $l$block_5: {
      // Inline function 'kotlinx.serialization.internal.equalsImpl' call
      if (this === other) {
        tmp$ret$0 = true;
        break $l$block_5;
      }
      if (!(other instanceof InlineClassDescriptor)) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.tg() === other.tg())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(other.mq_1 && contentEquals(this.yp(), other.yp()))) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.wg() === other.wg())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      var inductionVariable = 0;
      var last = this.wg();
      if (inductionVariable < last)
        do {
          var index = inductionVariable;
          inductionVariable = inductionVariable + 1 | 0;
          if (!(this.bh(index).tg() === other.bh(index).tg())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
          if (!equals(this.bh(index).ug(), other.bh(index).ug())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
        }
         while (inductionVariable < last);
      tmp$ret$0 = true;
    }
    return tmp$ret$0;
  };
  function InlinePrimitiveDescriptor$1($primitiveSerializer) {
    this.nq_1 = $primitiveSerializer;
  }
  protoOf(InlinePrimitiveDescriptor$1).oq = function () {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [this.nq_1];
  };
  protoOf(InlinePrimitiveDescriptor$1).of = function () {
    // Inline function 'kotlin.error' call
    var message = 'unsupported';
    throw IllegalStateException_init_$Create$(toString(message));
  };
  protoOf(InlinePrimitiveDescriptor$1).pf = function (encoder, value) {
    // Inline function 'kotlin.error' call
    var message = 'unsupported';
    throw IllegalStateException_init_$Create$(toString(message));
  };
  protoOf(InlinePrimitiveDescriptor$1).qf = function (decoder) {
    // Inline function 'kotlin.error' call
    var message = 'unsupported';
    throw IllegalStateException_init_$Create$(toString(message));
  };
  function missingFieldExceptionWithNewMessage(exception, message) {
    return exception.jg(message);
  }
  function jsonCachedSerialNames(_this__u8e3s4) {
    return cachedSerialNames(_this__u8e3s4);
  }
  function NoOpEncoder() {
    NoOpEncoder_instance = this;
    AbstractEncoder.call(this);
    this.qq_1 = EmptySerializersModule_0();
  }
  protoOf(NoOpEncoder).fj = function () {
    return this.qq_1;
  };
  protoOf(NoOpEncoder).kj = function (value) {
    return Unit_instance;
  };
  protoOf(NoOpEncoder).lj = function () {
    return Unit_instance;
  };
  protoOf(NoOpEncoder).mj = function (value) {
    return Unit_instance;
  };
  protoOf(NoOpEncoder).nj = function (value) {
    return Unit_instance;
  };
  protoOf(NoOpEncoder).oj = function (value) {
    return Unit_instance;
  };
  protoOf(NoOpEncoder).pj = function (value) {
    return Unit_instance;
  };
  protoOf(NoOpEncoder).qj = function (value) {
    return Unit_instance;
  };
  protoOf(NoOpEncoder).rj = function (value) {
    return Unit_instance;
  };
  protoOf(NoOpEncoder).sj = function (value) {
    return Unit_instance;
  };
  protoOf(NoOpEncoder).tj = function (value) {
    return Unit_instance;
  };
  protoOf(NoOpEncoder).uj = function (value) {
    return Unit_instance;
  };
  protoOf(NoOpEncoder).vj = function (enumDescriptor, index) {
    return Unit_instance;
  };
  var NoOpEncoder_instance;
  function NoOpEncoder_getInstance() {
    if (NoOpEncoder_instance == null)
      new NoOpEncoder();
    return NoOpEncoder_instance;
  }
  function error($this) {
    throw IllegalStateException_init_$Create$('Descriptor for type `kotlin.Nothing` does not have elements');
  }
  function NothingSerialDescriptor() {
    NothingSerialDescriptor_instance = this;
    this.rq_1 = OBJECT_getInstance();
    this.sq_1 = 'kotlin.Nothing';
  }
  protoOf(NothingSerialDescriptor).ug = function () {
    return this.rq_1;
  };
  protoOf(NothingSerialDescriptor).tg = function () {
    return this.sq_1;
  };
  protoOf(NothingSerialDescriptor).wg = function () {
    return 0;
  };
  protoOf(NothingSerialDescriptor).yg = function (index) {
    error(this);
  };
  protoOf(NothingSerialDescriptor).zg = function (name) {
    error(this);
  };
  protoOf(NothingSerialDescriptor).ch = function (index) {
    error(this);
  };
  protoOf(NothingSerialDescriptor).bh = function (index) {
    error(this);
  };
  protoOf(NothingSerialDescriptor).ah = function (index) {
    error(this);
  };
  protoOf(NothingSerialDescriptor).toString = function () {
    return 'NothingSerialDescriptor';
  };
  protoOf(NothingSerialDescriptor).equals = function (other) {
    return this === other;
  };
  protoOf(NothingSerialDescriptor).hashCode = function () {
    return getStringHashCode(this.sq_1) + imul(31, this.rq_1.hashCode()) | 0;
  };
  var NothingSerialDescriptor_instance;
  function NothingSerialDescriptor_getInstance() {
    if (NothingSerialDescriptor_instance == null)
      new NothingSerialDescriptor();
    return NothingSerialDescriptor_instance;
  }
  function NullableSerializer(serializer) {
    this.tq_1 = serializer;
    this.uq_1 = new SerialDescriptorForNullable(this.tq_1.of());
  }
  protoOf(NullableSerializer).of = function () {
    return this.uq_1;
  };
  protoOf(NullableSerializer).vq = function (encoder, value) {
    if (!(value == null)) {
      encoder.lk();
      encoder.ik(this.tq_1, value);
    } else {
      encoder.lj();
    }
  };
  protoOf(NullableSerializer).pf = function (encoder, value) {
    return this.vq(encoder, value);
  };
  protoOf(NullableSerializer).qf = function (decoder) {
    return decoder.bi() ? decoder.pi(this.tq_1) : decoder.ci();
  };
  protoOf(NullableSerializer).equals = function (other) {
    if (this === other)
      return true;
    if (other == null || !getKClassFromExpression(this).equals(getKClassFromExpression(other)))
      return false;
    if (!(other instanceof NullableSerializer))
      THROW_CCE();
    if (!equals(this.tq_1, other.tq_1))
      return false;
    return true;
  };
  protoOf(NullableSerializer).hashCode = function () {
    return hashCode(this.tq_1);
  };
  function SerialDescriptorForNullable(original) {
    this.dh_1 = original;
    this.eh_1 = this.dh_1.tg() + '?';
    this.fh_1 = cachedSerialNames(this.dh_1);
  }
  protoOf(SerialDescriptorForNullable).tg = function () {
    return this.eh_1;
  };
  protoOf(SerialDescriptorForNullable).zh = function () {
    return this.fh_1;
  };
  protoOf(SerialDescriptorForNullable).pg = function () {
    return true;
  };
  protoOf(SerialDescriptorForNullable).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof SerialDescriptorForNullable))
      return false;
    if (!equals(this.dh_1, other.dh_1))
      return false;
    return true;
  };
  protoOf(SerialDescriptorForNullable).toString = function () {
    return toString(this.dh_1) + '?';
  };
  protoOf(SerialDescriptorForNullable).hashCode = function () {
    return imul(hashCode(this.dh_1), 31);
  };
  protoOf(SerialDescriptorForNullable).ug = function () {
    return this.dh_1.ug();
  };
  protoOf(SerialDescriptorForNullable).vg = function () {
    return this.dh_1.vg();
  };
  protoOf(SerialDescriptorForNullable).wg = function () {
    return this.dh_1.wg();
  };
  protoOf(SerialDescriptorForNullable).xg = function () {
    return this.dh_1.xg();
  };
  protoOf(SerialDescriptorForNullable).yg = function (index) {
    return this.dh_1.yg(index);
  };
  protoOf(SerialDescriptorForNullable).zg = function (name) {
    return this.dh_1.zg(name);
  };
  protoOf(SerialDescriptorForNullable).ah = function (index) {
    return this.dh_1.ah(index);
  };
  protoOf(SerialDescriptorForNullable).bh = function (index) {
    return this.dh_1.bh(index);
  };
  protoOf(SerialDescriptorForNullable).ch = function (index) {
    return this.dh_1.ch(index);
  };
  function ObjectSerializer$descriptor$delegate$lambda$lambda(this$0) {
    return function ($this$buildSerialDescriptor) {
      $this$buildSerialDescriptor.tf_1 = this$0.xq_1;
      return Unit_instance;
    };
  }
  function ObjectSerializer$descriptor$delegate$lambda($serialName, this$0) {
    return function () {
      var tmp = OBJECT_getInstance();
      return buildSerialDescriptor($serialName, tmp, [], ObjectSerializer$descriptor$delegate$lambda$lambda(this$0));
    };
  }
  function ObjectSerializer$_get_descriptor_$ref_7z4xb6() {
    return constructCallableReference(function (p0) {
      return p0.of();
    }, 1, 0, 4);
  }
  function ObjectSerializer(serialName, objectInstance) {
    this.wq_1 = objectInstance;
    this.xq_1 = emptyList();
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp.yq_1 = lazy(tmp_0, ObjectSerializer$descriptor$delegate$lambda(serialName, this));
  }
  protoOf(ObjectSerializer).of = function () {
    var tmp0 = this.yq_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('descriptor', 1, tmp, ObjectSerializer$_get_descriptor_$ref_7z4xb6(), null);
    return tmp0.y1();
  };
  protoOf(ObjectSerializer).eg = function (encoder, value) {
    encoder.qi(this.of()).ri(this.of());
  };
  protoOf(ObjectSerializer).pf = function (encoder, value) {
    return this.eg(encoder, !(value == null) ? value : THROW_CCE());
  };
  protoOf(ObjectSerializer).qf = function (decoder) {
    // Inline function 'kotlinx.serialization.encoding.decodeStructure' call
    var descriptor = this.of();
    var composite = decoder.qi(descriptor);
    var tmp$ret$1;
    $l$block_0: {
      if (composite.gj()) {
        tmp$ret$1 = Unit_instance;
        break $l$block_0;
      }
      var index = composite.hj(this.of());
      if (index === -1) {
        tmp$ret$1 = Unit_instance;
        break $l$block_0;
      } else
        throw SerializationException_init_$Create$_0('Unexpected index ' + index);
    }
    var result = tmp$ret$1;
    composite.ri(descriptor);
    return this.wq_1;
  };
  function get_EMPTY_DESCRIPTOR_ARRAY() {
    _init_properties_Platform_common_kt__3qzecs();
    return EMPTY_DESCRIPTOR_ARRAY;
  }
  var EMPTY_DESCRIPTOR_ARRAY;
  function cachedSerialNames(_this__u8e3s4) {
    _init_properties_Platform_common_kt__3qzecs();
    if (isInterface(_this__u8e3s4, CachedNames))
      return _this__u8e3s4.zh();
    var result = HashSet_init_$Create$_1(_this__u8e3s4.wg());
    var inductionVariable = 0;
    var last = _this__u8e3s4.wg();
    if (inductionVariable < last)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        // Inline function 'kotlin.collections.plusAssign' call
        var element = _this__u8e3s4.yg(i);
        result.w(element);
      }
       while (inductionVariable < last);
    return result;
  }
  function compactArray(_this__u8e3s4) {
    _init_properties_Platform_common_kt__3qzecs();
    // Inline function 'kotlin.takeUnless' call
    var tmp;
    // Inline function 'kotlin.collections.isNullOrEmpty' call
    if (!(_this__u8e3s4 == null || _this__u8e3s4.p())) {
      tmp = _this__u8e3s4;
    } else {
      tmp = null;
    }
    var tmp0_safe_receiver = tmp;
    var tmp_0;
    if (tmp0_safe_receiver == null) {
      tmp_0 = null;
    } else {
      // Inline function 'kotlin.collections.toTypedArray' call
      tmp_0 = copyToArray(tmp0_safe_receiver);
    }
    var tmp1_elvis_lhs = tmp_0;
    return tmp1_elvis_lhs == null ? get_EMPTY_DESCRIPTOR_ARRAY() : tmp1_elvis_lhs;
  }
  function kclass(_this__u8e3s4) {
    _init_properties_Platform_common_kt__3qzecs();
    var t = _this__u8e3s4.h();
    var tmp;
    if (!(t == null) ? isInterface(t, KClass) : false) {
      tmp = t;
    } else {
      if (!(t == null) ? isInterface(t, KTypeParameter) : false) {
        throw IllegalArgumentException_init_$Create$('Captured type parameter ' + toString(t) + ' from generic non-reified function. ' + ('Such functionality cannot be supported because ' + toString(t) + ' is erased, either specify serializer explicitly or make ') + ('calling function inline with reified ' + toString(t) + '.'));
      } else {
        throw IllegalArgumentException_init_$Create$('Only KClass supported as classifier, got ' + toString_0(t));
      }
    }
    var tmp_0 = tmp;
    return isInterface(tmp_0, KClass) ? tmp_0 : THROW_CCE();
  }
  function typeOrThrow(_this__u8e3s4) {
    _init_properties_Platform_common_kt__3qzecs();
    var tmp0 = _this__u8e3s4.ar_1;
    var tmp$ret$0;
    $l$block: {
      // Inline function 'kotlin.requireNotNull' call
      if (tmp0 == null) {
        var message = 'Star projections in type arguments are not allowed, but had ' + toString_0(_this__u8e3s4.ar_1);
        throw IllegalArgumentException_init_$Create$(toString(message));
      } else {
        tmp$ret$0 = tmp0;
        break $l$block;
      }
    }
    return tmp$ret$0;
  }
  function notRegisteredMessage(_this__u8e3s4) {
    _init_properties_Platform_common_kt__3qzecs();
    var tmp0_elvis_lhs = _this__u8e3s4.m();
    return notRegisteredMessage_0(tmp0_elvis_lhs == null ? '<local class name not available>' : tmp0_elvis_lhs);
  }
  function notRegisteredMessage_0(className) {
    _init_properties_Platform_common_kt__3qzecs();
    return "Serializer for class '" + className + "' is not found.\n" + "Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.\n";
  }
  var properties_initialized_Platform_common_kt_i7q4ty;
  function _init_properties_Platform_common_kt__3qzecs() {
    if (!properties_initialized_Platform_common_kt_i7q4ty) {
      properties_initialized_Platform_common_kt_i7q4ty = true;
      // Inline function 'kotlin.arrayOf' call
      // Inline function 'kotlin.js.unsafeCast' call
      // Inline function 'kotlin.js.asDynamic' call
      EMPTY_DESCRIPTOR_ARRAY = [];
    }
  }
  function throwMissingFieldException(seen, goldenMask, descriptor) {
    // Inline function 'kotlin.collections.mutableListOf' call
    var missingFields = ArrayList_init_$Create$_0();
    var missingFieldsBits = goldenMask & ~seen;
    var inductionVariable = 0;
    if (inductionVariable < 32)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (!((missingFieldsBits & 1) === 0)) {
          // Inline function 'kotlin.collections.plusAssign' call
          var element = descriptor.yg(i);
          missingFields.w(element);
        }
        missingFieldsBits = missingFieldsBits >>> 1 | 0;
      }
       while (inductionVariable < 32);
    throw MissingFieldException_init_$Create$(missingFields, descriptor.tg());
  }
  function _get_childSerializers__7vnyfa($this) {
    var tmp0 = $this.fp_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('childSerializers', 1, tmp, PluginGeneratedSerialDescriptor$_get_childSerializers_$ref_e7suca(), null);
    return tmp0.y1();
  }
  function _get__hashCode__tgwhef_0($this) {
    var tmp0 = $this.hp_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('_hashCode', 1, tmp, PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz(), null);
    return tmp0.y1();
  }
  function buildIndices($this) {
    var indices = HashMap_init_$Create$();
    var inductionVariable = 0;
    var last = $this.ap_1.length - 1 | 0;
    if (inductionVariable <= last)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        // Inline function 'kotlin.collections.set' call
        var key = $this.ap_1[i];
        indices.h2(key, i);
      }
       while (inductionVariable <= last);
    return indices;
  }
  function PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this$0) {
    return function () {
      var tmp0_safe_receiver = this$0.xo_1;
      var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.oq();
      return tmp1_elvis_lhs == null ? get_EMPTY_SERIALIZER_ARRAY() : tmp1_elvis_lhs;
    };
  }
  function PluginGeneratedSerialDescriptor$_get_childSerializers_$ref_e7suca() {
    return constructCallableReference(function (p0) {
      return _get_childSerializers__7vnyfa(p0);
    }, 1, 0, 5);
  }
  function PluginGeneratedSerialDescriptor$typeParameterDescriptors$delegate$lambda(this$0) {
    return function () {
      var tmp0_safe_receiver = this$0.xo_1;
      var tmp1_safe_receiver = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.pq();
      var tmp;
      if (tmp1_safe_receiver == null) {
        tmp = null;
      } else {
        // Inline function 'kotlin.collections.map' call
        // Inline function 'kotlin.collections.mapTo' call
        var destination = ArrayList_init_$Create$(tmp1_safe_receiver.length);
        var inductionVariable = 0;
        var last = tmp1_safe_receiver.length;
        while (inductionVariable < last) {
          var item = tmp1_safe_receiver[inductionVariable];
          inductionVariable = inductionVariable + 1 | 0;
          var tmp$ret$2 = item.of();
          destination.w(tmp$ret$2);
        }
        tmp = destination;
      }
      return compactArray(tmp);
    };
  }
  function PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka() {
    return constructCallableReference(function (p0) {
      return p0.yp();
    }, 1, 0, 6);
  }
  function PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this$0) {
    return function () {
      return hashCodeImpl(this$0, this$0.yp());
    };
  }
  function PluginGeneratedSerialDescriptor$_get__hashCode_$ref_cmj4vz() {
    return constructCallableReference(function (p0) {
      return _get__hashCode__tgwhef_0(p0);
    }, 1, 0, 7);
  }
  function PluginGeneratedSerialDescriptor(serialName, generatedSerializer, elementsCount) {
    generatedSerializer = generatedSerializer === VOID ? null : generatedSerializer;
    this.wo_1 = serialName;
    this.xo_1 = generatedSerializer;
    this.yo_1 = elementsCount;
    this.zo_1 = -1;
    var tmp = this;
    var tmp_0 = 0;
    var tmp_1 = this.yo_1;
    // Inline function 'kotlin.arrayOfNulls' call
    var tmp_2 = Array(tmp_1);
    while (tmp_0 < tmp_1) {
      tmp_2[tmp_0] = '[UNINITIALIZED]';
      tmp_0 = tmp_0 + 1 | 0;
    }
    tmp.ap_1 = tmp_2;
    var tmp_3 = this;
    // Inline function 'kotlin.arrayOfNulls' call
    var size = this.yo_1;
    tmp_3.bp_1 = Array(size);
    this.cp_1 = null;
    this.dp_1 = booleanArray(this.yo_1);
    this.ep_1 = emptyMap();
    var tmp_4 = this;
    var tmp_5 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp_4.fp_1 = lazy(tmp_5, PluginGeneratedSerialDescriptor$childSerializers$delegate$lambda(this));
    var tmp_6 = this;
    var tmp_7 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp_6.gp_1 = lazy(tmp_7, PluginGeneratedSerialDescriptor$typeParameterDescriptors$delegate$lambda(this));
    var tmp_8 = this;
    var tmp_9 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp_8.hp_1 = lazy(tmp_9, PluginGeneratedSerialDescriptor$_hashCode$delegate$lambda(this));
  }
  protoOf(PluginGeneratedSerialDescriptor).tg = function () {
    return this.wo_1;
  };
  protoOf(PluginGeneratedSerialDescriptor).wg = function () {
    return this.yo_1;
  };
  protoOf(PluginGeneratedSerialDescriptor).ug = function () {
    return CLASS_getInstance();
  };
  protoOf(PluginGeneratedSerialDescriptor).xg = function () {
    var tmp0_elvis_lhs = this.cp_1;
    return tmp0_elvis_lhs == null ? emptyList() : tmp0_elvis_lhs;
  };
  protoOf(PluginGeneratedSerialDescriptor).zh = function () {
    return this.ep_1.c2();
  };
  protoOf(PluginGeneratedSerialDescriptor).yp = function () {
    var tmp0 = this.gp_1;
    var tmp = KProperty1;
    // Inline function 'kotlin.getValue' call
    getPropertyCallableRef('typeParameterDescriptors', 1, tmp, PluginGeneratedSerialDescriptor$_get_typeParameterDescriptors_$ref_jk3pka(), null);
    return tmp0.y1();
  };
  protoOf(PluginGeneratedSerialDescriptor).zp = function (name, isOptional) {
    this.zo_1 = this.zo_1 + 1 | 0;
    this.ap_1[this.zo_1] = name;
    this.dp_1[this.zo_1] = isOptional;
    this.bp_1[this.zo_1] = null;
    if (this.zo_1 === (this.yo_1 - 1 | 0)) {
      this.ep_1 = buildIndices(this);
    }
  };
  protoOf(PluginGeneratedSerialDescriptor).ip = function (name, isOptional, $super) {
    isOptional = isOptional === VOID ? false : isOptional;
    var tmp;
    if ($super === VOID) {
      this.zp(name, isOptional);
      tmp = Unit_instance;
    } else {
      tmp = $super.zp.call(this, name, isOptional);
    }
    return tmp;
  };
  protoOf(PluginGeneratedSerialDescriptor).bh = function (index) {
    return getChecked(_get_childSerializers__7vnyfa(this), index).of();
  };
  protoOf(PluginGeneratedSerialDescriptor).ch = function (index) {
    return getChecked_0(this.dp_1, index);
  };
  protoOf(PluginGeneratedSerialDescriptor).ah = function (index) {
    var tmp0_elvis_lhs = getChecked(this.bp_1, index);
    return tmp0_elvis_lhs == null ? emptyList() : tmp0_elvis_lhs;
  };
  protoOf(PluginGeneratedSerialDescriptor).yg = function (index) {
    return getChecked(this.ap_1, index);
  };
  protoOf(PluginGeneratedSerialDescriptor).zg = function (name) {
    var tmp0_elvis_lhs = this.ep_1.b2(name);
    return tmp0_elvis_lhs == null ? -3 : tmp0_elvis_lhs;
  };
  protoOf(PluginGeneratedSerialDescriptor).equals = function (other) {
    var tmp$ret$0;
    $l$block_5: {
      // Inline function 'kotlinx.serialization.internal.equalsImpl' call
      if (this === other) {
        tmp$ret$0 = true;
        break $l$block_5;
      }
      if (!(other instanceof PluginGeneratedSerialDescriptor)) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.tg() === other.tg())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!contentEquals(this.yp(), other.yp())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      if (!(this.wg() === other.wg())) {
        tmp$ret$0 = false;
        break $l$block_5;
      }
      var inductionVariable = 0;
      var last = this.wg();
      if (inductionVariable < last)
        do {
          var index = inductionVariable;
          inductionVariable = inductionVariable + 1 | 0;
          if (!(this.bh(index).tg() === other.bh(index).tg())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
          if (!equals(this.bh(index).ug(), other.bh(index).ug())) {
            tmp$ret$0 = false;
            break $l$block_5;
          }
        }
         while (inductionVariable < last);
      tmp$ret$0 = true;
    }
    return tmp$ret$0;
  };
  protoOf(PluginGeneratedSerialDescriptor).hashCode = function () {
    return _get__hashCode__tgwhef_0(this);
  };
  protoOf(PluginGeneratedSerialDescriptor).toString = function () {
    return toStringImpl(this);
  };
  function hashCodeImpl(_this__u8e3s4, typeParams) {
    var result = getStringHashCode(_this__u8e3s4.tg());
    result = imul(31, result) + contentHashCode(typeParams) | 0;
    var elementDescriptors = get_elementDescriptors(_this__u8e3s4);
    // Inline function 'kotlinx.serialization.internal.elementsHashCodeBy' call
    // Inline function 'kotlin.collections.fold' call
    var accumulator = 1;
    var _iterator__ex2g4s = elementDescriptors.r();
    while (_iterator__ex2g4s.s()) {
      var element = _iterator__ex2g4s.t();
      var hash = accumulator;
      var tmp = imul(31, hash);
      // Inline function 'kotlin.hashCode' call
      var tmp0_safe_receiver = element.tg();
      var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : hashCode(tmp0_safe_receiver);
      accumulator = tmp + (tmp1_elvis_lhs == null ? 0 : tmp1_elvis_lhs) | 0;
    }
    var namesHash = accumulator;
    // Inline function 'kotlinx.serialization.internal.elementsHashCodeBy' call
    // Inline function 'kotlin.collections.fold' call
    var accumulator_0 = 1;
    var _iterator__ex2g4s_0 = elementDescriptors.r();
    while (_iterator__ex2g4s_0.s()) {
      var element_0 = _iterator__ex2g4s_0.t();
      var hash_0 = accumulator_0;
      var tmp_0 = imul(31, hash_0);
      // Inline function 'kotlin.hashCode' call
      var tmp0_safe_receiver_0 = element_0.ug();
      var tmp1_elvis_lhs_0 = tmp0_safe_receiver_0 == null ? null : hashCode(tmp0_safe_receiver_0);
      accumulator_0 = tmp_0 + (tmp1_elvis_lhs_0 == null ? 0 : tmp1_elvis_lhs_0) | 0;
    }
    var kindHash = accumulator_0;
    result = imul(31, result) + namesHash | 0;
    result = imul(31, result) + kindHash | 0;
    return result;
  }
  function toStringImpl(_this__u8e3s4) {
    var tmp = until(0, _this__u8e3s4.wg());
    var tmp_0 = _this__u8e3s4.tg() + '(';
    return joinToString(tmp, ', ', tmp_0, ')', VOID, VOID, toStringImpl$lambda(_this__u8e3s4));
  }
  function toStringImpl$lambda($this_toStringImpl) {
    return function (i) {
      return $this_toStringImpl.yg(i) + ': ' + $this_toStringImpl.bh(i).tg();
    };
  }
  function get_EMPTY_SERIALIZER_ARRAY() {
    _init_properties_PluginHelperInterfaces_kt__xgvzfp();
    return EMPTY_SERIALIZER_ARRAY;
  }
  var EMPTY_SERIALIZER_ARRAY;
  function GeneratedSerializer() {
  }
  function SerializerFactory() {
  }
  var properties_initialized_PluginHelperInterfaces_kt_ap8in1;
  function _init_properties_PluginHelperInterfaces_kt__xgvzfp() {
    if (!properties_initialized_PluginHelperInterfaces_kt_ap8in1) {
      properties_initialized_PluginHelperInterfaces_kt_ap8in1 = true;
      // Inline function 'kotlin.arrayOf' call
      // Inline function 'kotlin.js.unsafeCast' call
      // Inline function 'kotlin.js.asDynamic' call
      EMPTY_SERIALIZER_ARRAY = [];
    }
  }
  function CharArraySerializer_0() {
    CharArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_9(Companion_getInstance_2()));
  }
  protoOf(CharArraySerializer_0).er = function (_this__u8e3s4) {
    return _this__u8e3s4.length;
  };
  protoOf(CharArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.er((!(_this__u8e3s4 == null) ? isCharArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(CharArraySerializer_0).fr = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _this__u8e3s4.length)
      return CharArrayBuilder_init_$Create$(_this__u8e3s4);
    return new CharArrayBuilder(copyOf(_this__u8e3s4, expectedSize), _this__u8e3s4.length);
  };
  protoOf(CharArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.fr((!(_this__u8e3s4 == null) ? isCharArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(CharArraySerializer_0).go = function (size) {
    return CharArrayBuilder_init_$Create$_0(size);
  };
  protoOf(CharArraySerializer_0).fo = function () {
    return CharArrayBuilder_init_$Create$_1();
  };
  protoOf(CharArraySerializer_0).gr = function (decoder, index, builder, checkIndex) {
    builder.jr(decoder.zi(this.yn_1, index));
  };
  protoOf(CharArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.gr(decoder, index, builder instanceof CharArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(CharArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.gr(decoder, index, builder instanceof CharArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(CharArraySerializer_0).kr = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        encoder.ek(this.yn_1, i, content[i]);
      }
       while (inductionVariable < size);
  };
  protoOf(CharArraySerializer_0).io = function (encoder, content, size) {
    return this.kr(encoder, (!(content == null) ? isCharArray(content) : false) ? content : THROW_CCE(), size);
  };
  var CharArraySerializer_instance;
  function CharArraySerializer_getInstance() {
    if (CharArraySerializer_instance == null)
      new CharArraySerializer_0();
    return CharArraySerializer_instance;
  }
  function DoubleArraySerializer_0() {
    DoubleArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_10(DoubleCompanionObject_instance));
  }
  protoOf(DoubleArraySerializer_0).nr = function (_this__u8e3s4) {
    return _this__u8e3s4.length;
  };
  protoOf(DoubleArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.nr((!(_this__u8e3s4 == null) ? isDoubleArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(DoubleArraySerializer_0).or = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _this__u8e3s4.length)
      return DoubleArrayBuilder_init_$Create$(_this__u8e3s4);
    return new DoubleArrayBuilder(copyOf_0(_this__u8e3s4, expectedSize), _this__u8e3s4.length);
  };
  protoOf(DoubleArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.or((!(_this__u8e3s4 == null) ? isDoubleArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(DoubleArraySerializer_0).go = function (size) {
    return DoubleArrayBuilder_init_$Create$_0(size);
  };
  protoOf(DoubleArraySerializer_0).fo = function () {
    return DoubleArrayBuilder_init_$Create$_1();
  };
  protoOf(DoubleArraySerializer_0).pr = function (decoder, index, builder, checkIndex) {
    builder.sr(decoder.yi(this.yn_1, index));
  };
  protoOf(DoubleArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.pr(decoder, index, builder instanceof DoubleArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(DoubleArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.pr(decoder, index, builder instanceof DoubleArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(DoubleArraySerializer_0).tr = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        encoder.dk(this.yn_1, i, content[i]);
      }
       while (inductionVariable < size);
  };
  protoOf(DoubleArraySerializer_0).io = function (encoder, content, size) {
    return this.tr(encoder, (!(content == null) ? isDoubleArray(content) : false) ? content : THROW_CCE(), size);
  };
  var DoubleArraySerializer_instance;
  function DoubleArraySerializer_getInstance() {
    if (DoubleArraySerializer_instance == null)
      new DoubleArraySerializer_0();
    return DoubleArraySerializer_instance;
  }
  function FloatArraySerializer_0() {
    FloatArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_11(FloatCompanionObject_instance));
  }
  protoOf(FloatArraySerializer_0).wr = function (_this__u8e3s4) {
    return _this__u8e3s4.length;
  };
  protoOf(FloatArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.wr((!(_this__u8e3s4 == null) ? isFloatArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(FloatArraySerializer_0).xr = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _this__u8e3s4.length)
      return FloatArrayBuilder_init_$Create$(_this__u8e3s4);
    return new FloatArrayBuilder(copyOf_1(_this__u8e3s4, expectedSize), _this__u8e3s4.length);
  };
  protoOf(FloatArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.xr((!(_this__u8e3s4 == null) ? isFloatArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(FloatArraySerializer_0).go = function (size) {
    return FloatArrayBuilder_init_$Create$_0(size);
  };
  protoOf(FloatArraySerializer_0).fo = function () {
    return FloatArrayBuilder_init_$Create$_1();
  };
  protoOf(FloatArraySerializer_0).yr = function (decoder, index, builder, checkIndex) {
    builder.bs(decoder.xi(this.yn_1, index));
  };
  protoOf(FloatArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.yr(decoder, index, builder instanceof FloatArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(FloatArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.yr(decoder, index, builder instanceof FloatArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(FloatArraySerializer_0).cs = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        encoder.ck(this.yn_1, i, content[i]);
      }
       while (inductionVariable < size);
  };
  protoOf(FloatArraySerializer_0).io = function (encoder, content, size) {
    return this.cs(encoder, (!(content == null) ? isFloatArray(content) : false) ? content : THROW_CCE(), size);
  };
  var FloatArraySerializer_instance;
  function FloatArraySerializer_getInstance() {
    if (FloatArraySerializer_instance == null)
      new FloatArraySerializer_0();
    return FloatArraySerializer_instance;
  }
  function LongArraySerializer_0() {
    LongArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_6(Companion_getInstance_3()));
  }
  protoOf(LongArraySerializer_0).fs = function (_this__u8e3s4) {
    return _this__u8e3s4.length;
  };
  protoOf(LongArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.fs((!(_this__u8e3s4 == null) ? isLongArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(LongArraySerializer_0).gs = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _this__u8e3s4.length)
      return LongArrayBuilder_init_$Create$(_this__u8e3s4);
    return new LongArrayBuilder(copyOf_2(_this__u8e3s4, expectedSize), _this__u8e3s4.length);
  };
  protoOf(LongArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.gs((!(_this__u8e3s4 == null) ? isLongArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(LongArraySerializer_0).go = function (size) {
    return LongArrayBuilder_init_$Create$_0(size);
  };
  protoOf(LongArraySerializer_0).fo = function () {
    return LongArrayBuilder_init_$Create$_1();
  };
  protoOf(LongArraySerializer_0).hs = function (decoder, index, builder, checkIndex) {
    builder.ks(decoder.wi(this.yn_1, index));
  };
  protoOf(LongArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.hs(decoder, index, builder instanceof LongArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(LongArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.hs(decoder, index, builder instanceof LongArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(LongArraySerializer_0).ls = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        encoder.bk(this.yn_1, i, content[i]);
      }
       while (inductionVariable < size);
  };
  protoOf(LongArraySerializer_0).io = function (encoder, content, size) {
    return this.ls(encoder, (!(content == null) ? isLongArray(content) : false) ? content : THROW_CCE(), size);
  };
  var LongArraySerializer_instance;
  function LongArraySerializer_getInstance() {
    if (LongArraySerializer_instance == null)
      new LongArraySerializer_0();
    return LongArraySerializer_instance;
  }
  function ULongArraySerializer_0() {
    ULongArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_2(Companion_getInstance_4()));
  }
  protoOf(ULongArraySerializer_0).os = function (_this__u8e3s4) {
    return _ULongArray___get_size__impl__ju6dtr(_this__u8e3s4);
  };
  protoOf(ULongArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.os(_this__u8e3s4 instanceof ULongArray ? _this__u8e3s4.df_1 : THROW_CCE());
  };
  protoOf(ULongArraySerializer_0).ps = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _ULongArray___get_size__impl__ju6dtr(_this__u8e3s4))
      return ULongArrayBuilder_init_$Create$(_this__u8e3s4);
    // Inline function 'kotlin.collections.copyOf' call
    var tmp$ret$0 = _ULongArray___init__impl__twm1l3(copyOf_2(_ULongArray___get_storage__impl__28e64j(_this__u8e3s4), expectedSize));
    return new ULongArrayBuilder(tmp$ret$0, _ULongArray___get_size__impl__ju6dtr(_this__u8e3s4));
  };
  protoOf(ULongArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.ps(_this__u8e3s4 instanceof ULongArray ? _this__u8e3s4.df_1 : THROW_CCE(), expectedSize);
  };
  protoOf(ULongArraySerializer_0).go = function (size) {
    return ULongArrayBuilder_init_$Create$_0(size);
  };
  protoOf(ULongArraySerializer_0).fo = function () {
    return ULongArrayBuilder_init_$Create$_1();
  };
  protoOf(ULongArraySerializer_0).qs = function (decoder, index, builder, checkIndex) {
    // Inline function 'kotlin.toULong' call
    var this_0 = decoder.bj(this.yn_1, index).hi();
    var tmp$ret$0 = _ULong___init__impl__c78o9k(this_0);
    builder.ts(tmp$ret$0);
  };
  protoOf(ULongArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.qs(decoder, index, builder instanceof ULongArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(ULongArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.qs(decoder, index, builder instanceof ULongArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(ULongArraySerializer_0).us = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        var tmp = encoder.gk(this.yn_1, i);
        // Inline function 'kotlin.ULong.toLong' call
        var this_0 = ULongArray__get_impl_pr71q9(content, i);
        var tmp$ret$0 = _ULong___get_data__impl__fggpzb(this_0);
        tmp.qj(tmp$ret$0);
      }
       while (inductionVariable < size);
  };
  protoOf(ULongArraySerializer_0).io = function (encoder, content, size) {
    return this.us(encoder, content instanceof ULongArray ? content.df_1 : THROW_CCE(), size);
  };
  var ULongArraySerializer_instance;
  function ULongArraySerializer_getInstance() {
    if (ULongArraySerializer_instance == null)
      new ULongArraySerializer_0();
    return ULongArraySerializer_instance;
  }
  function IntArraySerializer_0() {
    IntArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_5(IntCompanionObject_instance));
  }
  protoOf(IntArraySerializer_0).xs = function (_this__u8e3s4) {
    return _this__u8e3s4.length;
  };
  protoOf(IntArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.xs((!(_this__u8e3s4 == null) ? isIntArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(IntArraySerializer_0).ys = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _this__u8e3s4.length)
      return IntArrayBuilder_init_$Create$(_this__u8e3s4);
    return new IntArrayBuilder(copyOf_3(_this__u8e3s4, expectedSize), _this__u8e3s4.length);
  };
  protoOf(IntArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.ys((!(_this__u8e3s4 == null) ? isIntArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(IntArraySerializer_0).go = function (size) {
    return IntArrayBuilder_init_$Create$_0(size);
  };
  protoOf(IntArraySerializer_0).fo = function () {
    return IntArrayBuilder_init_$Create$_1();
  };
  protoOf(IntArraySerializer_0).zs = function (decoder, index, builder, checkIndex) {
    builder.ct(decoder.vi(this.yn_1, index));
  };
  protoOf(IntArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.zs(decoder, index, builder instanceof IntArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(IntArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.zs(decoder, index, builder instanceof IntArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(IntArraySerializer_0).dt = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        encoder.ak(this.yn_1, i, content[i]);
      }
       while (inductionVariable < size);
  };
  protoOf(IntArraySerializer_0).io = function (encoder, content, size) {
    return this.dt(encoder, (!(content == null) ? isIntArray(content) : false) ? content : THROW_CCE(), size);
  };
  var IntArraySerializer_instance;
  function IntArraySerializer_getInstance() {
    if (IntArraySerializer_instance == null)
      new IntArraySerializer_0();
    return IntArraySerializer_instance;
  }
  function UIntArraySerializer_0() {
    UIntArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_1(Companion_getInstance_5()));
  }
  protoOf(UIntArraySerializer_0).gt = function (_this__u8e3s4) {
    return _UIntArray___get_size__impl__r6l8ci(_this__u8e3s4);
  };
  protoOf(UIntArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.gt(_this__u8e3s4 instanceof UIntArray ? _this__u8e3s4.te_1 : THROW_CCE());
  };
  protoOf(UIntArraySerializer_0).ht = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _UIntArray___get_size__impl__r6l8ci(_this__u8e3s4))
      return UIntArrayBuilder_init_$Create$(_this__u8e3s4);
    // Inline function 'kotlin.collections.copyOf' call
    var tmp$ret$0 = _UIntArray___init__impl__ghjpc6(copyOf_3(_UIntArray___get_storage__impl__92a0v0(_this__u8e3s4), expectedSize));
    return new UIntArrayBuilder(tmp$ret$0, _UIntArray___get_size__impl__r6l8ci(_this__u8e3s4));
  };
  protoOf(UIntArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.ht(_this__u8e3s4 instanceof UIntArray ? _this__u8e3s4.te_1 : THROW_CCE(), expectedSize);
  };
  protoOf(UIntArraySerializer_0).go = function (size) {
    return UIntArrayBuilder_init_$Create$_0(size);
  };
  protoOf(UIntArraySerializer_0).fo = function () {
    return UIntArrayBuilder_init_$Create$_1();
  };
  protoOf(UIntArraySerializer_0).it = function (decoder, index, builder, checkIndex) {
    // Inline function 'kotlin.toUInt' call
    var this_0 = decoder.bj(this.yn_1, index).gi();
    var tmp$ret$0 = _UInt___init__impl__l7qpdl(this_0);
    builder.lt(tmp$ret$0);
  };
  protoOf(UIntArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.it(decoder, index, builder instanceof UIntArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(UIntArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.it(decoder, index, builder instanceof UIntArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(UIntArraySerializer_0).mt = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        var tmp = encoder.gk(this.yn_1, i);
        // Inline function 'kotlin.UInt.toInt' call
        var this_0 = UIntArray__get_impl_gp5kza(content, i);
        var tmp$ret$0 = _UInt___get_data__impl__f0vqqw(this_0);
        tmp.pj(tmp$ret$0);
      }
       while (inductionVariable < size);
  };
  protoOf(UIntArraySerializer_0).io = function (encoder, content, size) {
    return this.mt(encoder, content instanceof UIntArray ? content.te_1 : THROW_CCE(), size);
  };
  var UIntArraySerializer_instance;
  function UIntArraySerializer_getInstance() {
    if (UIntArraySerializer_instance == null)
      new UIntArraySerializer_0();
    return UIntArraySerializer_instance;
  }
  function ShortArraySerializer_0() {
    ShortArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_8(ShortCompanionObject_instance));
  }
  protoOf(ShortArraySerializer_0).pt = function (_this__u8e3s4) {
    return _this__u8e3s4.length;
  };
  protoOf(ShortArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.pt((!(_this__u8e3s4 == null) ? isShortArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(ShortArraySerializer_0).qt = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _this__u8e3s4.length)
      return ShortArrayBuilder_init_$Create$(_this__u8e3s4);
    return new ShortArrayBuilder(copyOf_4(_this__u8e3s4, expectedSize), _this__u8e3s4.length);
  };
  protoOf(ShortArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.qt((!(_this__u8e3s4 == null) ? isShortArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(ShortArraySerializer_0).go = function (size) {
    return ShortArrayBuilder_init_$Create$_0(size);
  };
  protoOf(ShortArraySerializer_0).fo = function () {
    return ShortArrayBuilder_init_$Create$_1();
  };
  protoOf(ShortArraySerializer_0).rt = function (decoder, index, builder, checkIndex) {
    builder.ut(decoder.ui(this.yn_1, index));
  };
  protoOf(ShortArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.rt(decoder, index, builder instanceof ShortArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(ShortArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.rt(decoder, index, builder instanceof ShortArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(ShortArraySerializer_0).vt = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        encoder.zj(this.yn_1, i, content[i]);
      }
       while (inductionVariable < size);
  };
  protoOf(ShortArraySerializer_0).io = function (encoder, content, size) {
    return this.vt(encoder, (!(content == null) ? isShortArray(content) : false) ? content : THROW_CCE(), size);
  };
  var ShortArraySerializer_instance;
  function ShortArraySerializer_getInstance() {
    if (ShortArraySerializer_instance == null)
      new ShortArraySerializer_0();
    return ShortArraySerializer_instance;
  }
  function UShortArraySerializer_0() {
    UShortArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_4(Companion_getInstance_6()));
  }
  protoOf(UShortArraySerializer_0).yt = function (_this__u8e3s4) {
    return _UShortArray___get_size__impl__jqto1b(_this__u8e3s4);
  };
  protoOf(UShortArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.yt(_this__u8e3s4 instanceof UShortArray ? _this__u8e3s4.nf_1 : THROW_CCE());
  };
  protoOf(UShortArraySerializer_0).zt = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _UShortArray___get_size__impl__jqto1b(_this__u8e3s4))
      return UShortArrayBuilder_init_$Create$(_this__u8e3s4);
    // Inline function 'kotlin.collections.copyOf' call
    var tmp$ret$0 = _UShortArray___init__impl__9b26ef(copyOf_4(_UShortArray___get_storage__impl__t2jpv5(_this__u8e3s4), expectedSize));
    return new UShortArrayBuilder(tmp$ret$0, _UShortArray___get_size__impl__jqto1b(_this__u8e3s4));
  };
  protoOf(UShortArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.zt(_this__u8e3s4 instanceof UShortArray ? _this__u8e3s4.nf_1 : THROW_CCE(), expectedSize);
  };
  protoOf(UShortArraySerializer_0).go = function (size) {
    return UShortArrayBuilder_init_$Create$_0(size);
  };
  protoOf(UShortArraySerializer_0).fo = function () {
    return UShortArrayBuilder_init_$Create$_1();
  };
  protoOf(UShortArraySerializer_0).au = function (decoder, index, builder, checkIndex) {
    // Inline function 'kotlin.toUShort' call
    var this_0 = decoder.bj(this.yn_1, index).fi();
    var tmp$ret$0 = _UShort___init__impl__jigrne(this_0);
    builder.du(tmp$ret$0);
  };
  protoOf(UShortArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.au(decoder, index, builder instanceof UShortArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(UShortArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.au(decoder, index, builder instanceof UShortArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(UShortArraySerializer_0).eu = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        var tmp = encoder.gk(this.yn_1, i);
        // Inline function 'kotlin.UShort.toShort' call
        var this_0 = UShortArray__get_impl_fnbhmx(content, i);
        var tmp$ret$0 = _UShort___get_data__impl__g0245(this_0);
        tmp.oj(tmp$ret$0);
      }
       while (inductionVariable < size);
  };
  protoOf(UShortArraySerializer_0).io = function (encoder, content, size) {
    return this.eu(encoder, content instanceof UShortArray ? content.nf_1 : THROW_CCE(), size);
  };
  var UShortArraySerializer_instance;
  function UShortArraySerializer_getInstance() {
    if (UShortArraySerializer_instance == null)
      new UShortArraySerializer_0();
    return UShortArraySerializer_instance;
  }
  function ByteArraySerializer_0() {
    ByteArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_7(ByteCompanionObject_instance));
  }
  protoOf(ByteArraySerializer_0).hu = function (_this__u8e3s4) {
    return _this__u8e3s4.length;
  };
  protoOf(ByteArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.hu((!(_this__u8e3s4 == null) ? isByteArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(ByteArraySerializer_0).iu = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _this__u8e3s4.length)
      return ByteArrayBuilder_init_$Create$(_this__u8e3s4);
    return new ByteArrayBuilder(copyOf_5(_this__u8e3s4, expectedSize), _this__u8e3s4.length);
  };
  protoOf(ByteArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.iu((!(_this__u8e3s4 == null) ? isByteArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(ByteArraySerializer_0).go = function (size) {
    return ByteArrayBuilder_init_$Create$_0(size);
  };
  protoOf(ByteArraySerializer_0).fo = function () {
    return ByteArrayBuilder_init_$Create$_1();
  };
  protoOf(ByteArraySerializer_0).ju = function (decoder, index, builder, checkIndex) {
    builder.mu(decoder.ti(this.yn_1, index));
  };
  protoOf(ByteArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.ju(decoder, index, builder instanceof ByteArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(ByteArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.ju(decoder, index, builder instanceof ByteArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(ByteArraySerializer_0).nu = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        encoder.yj(this.yn_1, i, content[i]);
      }
       while (inductionVariable < size);
  };
  protoOf(ByteArraySerializer_0).io = function (encoder, content, size) {
    return this.nu(encoder, (!(content == null) ? isByteArray(content) : false) ? content : THROW_CCE(), size);
  };
  var ByteArraySerializer_instance;
  function ByteArraySerializer_getInstance() {
    if (ByteArraySerializer_instance == null)
      new ByteArraySerializer_0();
    return ByteArraySerializer_instance;
  }
  function UByteArraySerializer_0() {
    UByteArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_3(Companion_getInstance_7()));
  }
  protoOf(UByteArraySerializer_0).qu = function (_this__u8e3s4) {
    return _UByteArray___get_size__impl__h6pkdv(_this__u8e3s4);
  };
  protoOf(UByteArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.qu(_this__u8e3s4 instanceof UByteArray ? _this__u8e3s4.je_1 : THROW_CCE());
  };
  protoOf(UByteArraySerializer_0).ru = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _UByteArray___get_size__impl__h6pkdv(_this__u8e3s4))
      return UByteArrayBuilder_init_$Create$(_this__u8e3s4);
    // Inline function 'kotlin.collections.copyOf' call
    var tmp$ret$0 = _UByteArray___init__impl__ip4y9n(copyOf_5(_UByteArray___get_storage__impl__d4kctt(_this__u8e3s4), expectedSize));
    return new UByteArrayBuilder(tmp$ret$0, _UByteArray___get_size__impl__h6pkdv(_this__u8e3s4));
  };
  protoOf(UByteArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.ru(_this__u8e3s4 instanceof UByteArray ? _this__u8e3s4.je_1 : THROW_CCE(), expectedSize);
  };
  protoOf(UByteArraySerializer_0).go = function (size) {
    return UByteArrayBuilder_init_$Create$_0(size);
  };
  protoOf(UByteArraySerializer_0).fo = function () {
    return UByteArrayBuilder_init_$Create$_1();
  };
  protoOf(UByteArraySerializer_0).su = function (decoder, index, builder, checkIndex) {
    // Inline function 'kotlin.toUByte' call
    var this_0 = decoder.bj(this.yn_1, index).ei();
    var tmp$ret$0 = _UByte___init__impl__g9hnc4(this_0);
    builder.vu(tmp$ret$0);
  };
  protoOf(UByteArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.su(decoder, index, builder instanceof UByteArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(UByteArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.su(decoder, index, builder instanceof UByteArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(UByteArraySerializer_0).wu = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        var tmp = encoder.gk(this.yn_1, i);
        // Inline function 'kotlin.UByte.toByte' call
        var this_0 = UByteArray__get_impl_t5f3hv(content, i);
        var tmp$ret$0 = _UByte___get_data__impl__jof9qr(this_0);
        tmp.nj(tmp$ret$0);
      }
       while (inductionVariable < size);
  };
  protoOf(UByteArraySerializer_0).io = function (encoder, content, size) {
    return this.wu(encoder, content instanceof UByteArray ? content.je_1 : THROW_CCE(), size);
  };
  var UByteArraySerializer_instance;
  function UByteArraySerializer_getInstance() {
    if (UByteArraySerializer_instance == null)
      new UByteArraySerializer_0();
    return UByteArraySerializer_instance;
  }
  function BooleanArraySerializer_0() {
    BooleanArraySerializer_instance = this;
    PrimitiveArraySerializer.call(this, serializer_12(BooleanCompanionObject_instance));
  }
  protoOf(BooleanArraySerializer_0).zu = function (_this__u8e3s4) {
    return _this__u8e3s4.length;
  };
  protoOf(BooleanArraySerializer_0).zm = function (_this__u8e3s4) {
    return this.zu((!(_this__u8e3s4 == null) ? isBooleanArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(BooleanArraySerializer_0).av = function (_this__u8e3s4, expectedSize) {
    if (expectedSize === _this__u8e3s4.length)
      return BooleanArrayBuilder_init_$Create$(_this__u8e3s4);
    return new BooleanArrayBuilder(copyOf_6(_this__u8e3s4, expectedSize), _this__u8e3s4.length);
  };
  protoOf(BooleanArraySerializer_0).yl = function (_this__u8e3s4, expectedSize) {
    return this.av((!(_this__u8e3s4 == null) ? isBooleanArray(_this__u8e3s4) : false) ? _this__u8e3s4 : THROW_CCE(), expectedSize);
  };
  protoOf(BooleanArraySerializer_0).go = function (size) {
    return BooleanArrayBuilder_init_$Create$_0(size);
  };
  protoOf(BooleanArraySerializer_0).fo = function () {
    return BooleanArrayBuilder_init_$Create$_1();
  };
  protoOf(BooleanArraySerializer_0).bv = function (decoder, index, builder, checkIndex) {
    builder.ev(decoder.si(this.yn_1, index));
  };
  protoOf(BooleanArraySerializer_0).hm = function (decoder, index, builder, checkIndex) {
    return this.bv(decoder, index, builder instanceof BooleanArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(BooleanArraySerializer_0).ho = function (decoder, index, builder, checkIndex) {
    return this.bv(decoder, index, builder instanceof BooleanArrayBuilder ? builder : THROW_CCE(), checkIndex);
  };
  protoOf(BooleanArraySerializer_0).fv = function (encoder, content, size) {
    var inductionVariable = 0;
    if (inductionVariable < size)
      do {
        var i = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        encoder.xj(this.yn_1, i, content[i]);
      }
       while (inductionVariable < size);
  };
  protoOf(BooleanArraySerializer_0).io = function (encoder, content, size) {
    return this.fv(encoder, (!(content == null) ? isBooleanArray(content) : false) ? content : THROW_CCE(), size);
  };
  var BooleanArraySerializer_instance;
  function BooleanArraySerializer_getInstance() {
    if (BooleanArraySerializer_instance == null)
      new BooleanArraySerializer_0();
    return BooleanArraySerializer_instance;
  }
  function CharArrayBuilder_init_$Init$(bufferWithData, $this) {
    CharArrayBuilder.call($this, bufferWithData, bufferWithData.length);
    return $this;
  }
  function CharArrayBuilder_init_$Create$(bufferWithData) {
    return CharArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(CharArrayBuilder)));
  }
  function CharArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    CharArrayBuilder.call($this, charArray(initialCapacity), 0);
    return $this;
  }
  function CharArrayBuilder_init_$Create$_0(initialCapacity) {
    return CharArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(CharArrayBuilder)));
  }
  function CharArrayBuilder_init_$Init$_1($this) {
    CharArrayBuilder.call($this, charArray(10), 0);
    return $this;
  }
  function CharArrayBuilder_init_$Create$_1() {
    return CharArrayBuilder_init_$Init$_1(objectCreate(protoOf(CharArrayBuilder)));
  }
  function CharArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.hr_1 = bufferWithData;
    this.ir_1 = initialPosition;
  }
  protoOf(CharArrayBuilder).ao = function () {
    return this.ir_1;
  };
  protoOf(CharArrayBuilder).ko = function (requiredCapacity) {
    if (this.hr_1.length < requiredCapacity)
      this.hr_1 = copyOf(this.hr_1, coerceAtLeast(requiredCapacity, imul(this.hr_1.length, 2)));
  };
  protoOf(CharArrayBuilder).jr = function (c) {
    this.lo();
    var tmp = this.hr_1;
    var _unary__edvuaz = this.ir_1;
    this.ir_1 = _unary__edvuaz + 1 | 0;
    tmp[_unary__edvuaz] = c;
  };
  protoOf(CharArrayBuilder).co = function () {
    return copyOf(this.hr_1, this.ir_1);
  };
  function DoubleArrayBuilder_init_$Init$(bufferWithData, $this) {
    DoubleArrayBuilder.call($this, bufferWithData, bufferWithData.length);
    return $this;
  }
  function DoubleArrayBuilder_init_$Create$(bufferWithData) {
    return DoubleArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(DoubleArrayBuilder)));
  }
  function DoubleArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    DoubleArrayBuilder.call($this, new Float64Array(initialCapacity), 0);
    return $this;
  }
  function DoubleArrayBuilder_init_$Create$_0(initialCapacity) {
    return DoubleArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(DoubleArrayBuilder)));
  }
  function DoubleArrayBuilder_init_$Init$_1($this) {
    DoubleArrayBuilder.call($this, new Float64Array(10), 0);
    return $this;
  }
  function DoubleArrayBuilder_init_$Create$_1() {
    return DoubleArrayBuilder_init_$Init$_1(objectCreate(protoOf(DoubleArrayBuilder)));
  }
  function DoubleArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.qr_1 = bufferWithData;
    this.rr_1 = initialPosition;
  }
  protoOf(DoubleArrayBuilder).ao = function () {
    return this.rr_1;
  };
  protoOf(DoubleArrayBuilder).ko = function (requiredCapacity) {
    if (this.qr_1.length < requiredCapacity)
      this.qr_1 = copyOf_0(this.qr_1, coerceAtLeast(requiredCapacity, imul(this.qr_1.length, 2)));
  };
  protoOf(DoubleArrayBuilder).sr = function (c) {
    this.lo();
    var tmp = this.qr_1;
    var _unary__edvuaz = this.rr_1;
    this.rr_1 = _unary__edvuaz + 1 | 0;
    tmp[_unary__edvuaz] = c;
  };
  protoOf(DoubleArrayBuilder).co = function () {
    return copyOf_0(this.qr_1, this.rr_1);
  };
  function FloatArrayBuilder_init_$Init$(bufferWithData, $this) {
    FloatArrayBuilder.call($this, bufferWithData, bufferWithData.length);
    return $this;
  }
  function FloatArrayBuilder_init_$Create$(bufferWithData) {
    return FloatArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(FloatArrayBuilder)));
  }
  function FloatArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    FloatArrayBuilder.call($this, new Float32Array(initialCapacity), 0);
    return $this;
  }
  function FloatArrayBuilder_init_$Create$_0(initialCapacity) {
    return FloatArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(FloatArrayBuilder)));
  }
  function FloatArrayBuilder_init_$Init$_1($this) {
    FloatArrayBuilder.call($this, new Float32Array(10), 0);
    return $this;
  }
  function FloatArrayBuilder_init_$Create$_1() {
    return FloatArrayBuilder_init_$Init$_1(objectCreate(protoOf(FloatArrayBuilder)));
  }
  function FloatArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.zr_1 = bufferWithData;
    this.as_1 = initialPosition;
  }
  protoOf(FloatArrayBuilder).ao = function () {
    return this.as_1;
  };
  protoOf(FloatArrayBuilder).ko = function (requiredCapacity) {
    if (this.zr_1.length < requiredCapacity)
      this.zr_1 = copyOf_1(this.zr_1, coerceAtLeast(requiredCapacity, imul(this.zr_1.length, 2)));
  };
  protoOf(FloatArrayBuilder).bs = function (c) {
    this.lo();
    var tmp = this.zr_1;
    var _unary__edvuaz = this.as_1;
    this.as_1 = _unary__edvuaz + 1 | 0;
    tmp[_unary__edvuaz] = c;
  };
  protoOf(FloatArrayBuilder).co = function () {
    return copyOf_1(this.zr_1, this.as_1);
  };
  function LongArrayBuilder_init_$Init$(bufferWithData, $this) {
    LongArrayBuilder.call($this, bufferWithData, bufferWithData.length);
    return $this;
  }
  function LongArrayBuilder_init_$Create$(bufferWithData) {
    return LongArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(LongArrayBuilder)));
  }
  function LongArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    LongArrayBuilder.call($this, longArray(initialCapacity), 0);
    return $this;
  }
  function LongArrayBuilder_init_$Create$_0(initialCapacity) {
    return LongArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(LongArrayBuilder)));
  }
  function LongArrayBuilder_init_$Init$_1($this) {
    LongArrayBuilder.call($this, longArray(10), 0);
    return $this;
  }
  function LongArrayBuilder_init_$Create$_1() {
    return LongArrayBuilder_init_$Init$_1(objectCreate(protoOf(LongArrayBuilder)));
  }
  function LongArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.is_1 = bufferWithData;
    this.js_1 = initialPosition;
  }
  protoOf(LongArrayBuilder).ao = function () {
    return this.js_1;
  };
  protoOf(LongArrayBuilder).ko = function (requiredCapacity) {
    if (this.is_1.length < requiredCapacity)
      this.is_1 = copyOf_2(this.is_1, coerceAtLeast(requiredCapacity, imul(this.is_1.length, 2)));
  };
  protoOf(LongArrayBuilder).ks = function (c) {
    this.lo();
    var tmp = this.is_1;
    var _unary__edvuaz = this.js_1;
    this.js_1 = _unary__edvuaz + 1 | 0;
    tmp[_unary__edvuaz] = c;
  };
  protoOf(LongArrayBuilder).co = function () {
    return copyOf_2(this.is_1, this.js_1);
  };
  function ULongArrayBuilder_init_$Init$(bufferWithData, $this) {
    ULongArrayBuilder.call($this, bufferWithData, _ULongArray___get_size__impl__ju6dtr(bufferWithData));
    return $this;
  }
  function ULongArrayBuilder_init_$Create$(bufferWithData) {
    return ULongArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(ULongArrayBuilder)));
  }
  function ULongArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    ULongArrayBuilder.call($this, _ULongArray___init__impl__twm1l3_0(initialCapacity), 0);
    return $this;
  }
  function ULongArrayBuilder_init_$Create$_0(initialCapacity) {
    return ULongArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(ULongArrayBuilder)));
  }
  function ULongArrayBuilder_init_$Init$_1($this) {
    ULongArrayBuilder.call($this, _ULongArray___init__impl__twm1l3_0(10), 0);
    return $this;
  }
  function ULongArrayBuilder_init_$Create$_1() {
    return ULongArrayBuilder_init_$Init$_1(objectCreate(protoOf(ULongArrayBuilder)));
  }
  function ULongArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.rs_1 = bufferWithData;
    this.ss_1 = initialPosition;
  }
  protoOf(ULongArrayBuilder).ao = function () {
    return this.ss_1;
  };
  protoOf(ULongArrayBuilder).ko = function (requiredCapacity) {
    if (_ULongArray___get_size__impl__ju6dtr(this.rs_1) < requiredCapacity) {
      var tmp = this;
      var tmp0 = this.rs_1;
      // Inline function 'kotlin.collections.copyOf' call
      var newSize = coerceAtLeast(requiredCapacity, imul(_ULongArray___get_size__impl__ju6dtr(this.rs_1), 2));
      tmp.rs_1 = _ULongArray___init__impl__twm1l3(copyOf_2(_ULongArray___get_storage__impl__28e64j(tmp0), newSize));
    }
  };
  protoOf(ULongArrayBuilder).ts = function (c) {
    this.lo();
    var tmp = this.rs_1;
    var _unary__edvuaz = this.ss_1;
    this.ss_1 = _unary__edvuaz + 1 | 0;
    ULongArray__set_impl_z19mvh(tmp, _unary__edvuaz, c);
  };
  protoOf(ULongArrayBuilder).gv = function () {
    var tmp0 = this.rs_1;
    // Inline function 'kotlin.collections.copyOf' call
    var newSize = this.ss_1;
    return _ULongArray___init__impl__twm1l3(copyOf_2(_ULongArray___get_storage__impl__28e64j(tmp0), newSize));
  };
  protoOf(ULongArrayBuilder).co = function () {
    return new ULongArray(this.gv());
  };
  function IntArrayBuilder_init_$Init$(bufferWithData, $this) {
    IntArrayBuilder.call($this, bufferWithData, bufferWithData.length);
    return $this;
  }
  function IntArrayBuilder_init_$Create$(bufferWithData) {
    return IntArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(IntArrayBuilder)));
  }
  function IntArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    IntArrayBuilder.call($this, new Int32Array(initialCapacity), 0);
    return $this;
  }
  function IntArrayBuilder_init_$Create$_0(initialCapacity) {
    return IntArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(IntArrayBuilder)));
  }
  function IntArrayBuilder_init_$Init$_1($this) {
    IntArrayBuilder.call($this, new Int32Array(10), 0);
    return $this;
  }
  function IntArrayBuilder_init_$Create$_1() {
    return IntArrayBuilder_init_$Init$_1(objectCreate(protoOf(IntArrayBuilder)));
  }
  function IntArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.at_1 = bufferWithData;
    this.bt_1 = initialPosition;
  }
  protoOf(IntArrayBuilder).ao = function () {
    return this.bt_1;
  };
  protoOf(IntArrayBuilder).ko = function (requiredCapacity) {
    if (this.at_1.length < requiredCapacity)
      this.at_1 = copyOf_3(this.at_1, coerceAtLeast(requiredCapacity, imul(this.at_1.length, 2)));
  };
  protoOf(IntArrayBuilder).ct = function (c) {
    this.lo();
    var tmp = this.at_1;
    var _unary__edvuaz = this.bt_1;
    this.bt_1 = _unary__edvuaz + 1 | 0;
    tmp[_unary__edvuaz] = c;
  };
  protoOf(IntArrayBuilder).co = function () {
    return copyOf_3(this.at_1, this.bt_1);
  };
  function UIntArrayBuilder_init_$Init$(bufferWithData, $this) {
    UIntArrayBuilder.call($this, bufferWithData, _UIntArray___get_size__impl__r6l8ci(bufferWithData));
    return $this;
  }
  function UIntArrayBuilder_init_$Create$(bufferWithData) {
    return UIntArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(UIntArrayBuilder)));
  }
  function UIntArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    UIntArrayBuilder.call($this, _UIntArray___init__impl__ghjpc6_0(initialCapacity), 0);
    return $this;
  }
  function UIntArrayBuilder_init_$Create$_0(initialCapacity) {
    return UIntArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(UIntArrayBuilder)));
  }
  function UIntArrayBuilder_init_$Init$_1($this) {
    UIntArrayBuilder.call($this, _UIntArray___init__impl__ghjpc6_0(10), 0);
    return $this;
  }
  function UIntArrayBuilder_init_$Create$_1() {
    return UIntArrayBuilder_init_$Init$_1(objectCreate(protoOf(UIntArrayBuilder)));
  }
  function UIntArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.jt_1 = bufferWithData;
    this.kt_1 = initialPosition;
  }
  protoOf(UIntArrayBuilder).ao = function () {
    return this.kt_1;
  };
  protoOf(UIntArrayBuilder).ko = function (requiredCapacity) {
    if (_UIntArray___get_size__impl__r6l8ci(this.jt_1) < requiredCapacity) {
      var tmp = this;
      var tmp0 = this.jt_1;
      // Inline function 'kotlin.collections.copyOf' call
      var newSize = coerceAtLeast(requiredCapacity, imul(_UIntArray___get_size__impl__r6l8ci(this.jt_1), 2));
      tmp.jt_1 = _UIntArray___init__impl__ghjpc6(copyOf_3(_UIntArray___get_storage__impl__92a0v0(tmp0), newSize));
    }
  };
  protoOf(UIntArrayBuilder).lt = function (c) {
    this.lo();
    var tmp = this.jt_1;
    var _unary__edvuaz = this.kt_1;
    this.kt_1 = _unary__edvuaz + 1 | 0;
    UIntArray__set_impl_7f2zu2(tmp, _unary__edvuaz, c);
  };
  protoOf(UIntArrayBuilder).hv = function () {
    var tmp0 = this.jt_1;
    // Inline function 'kotlin.collections.copyOf' call
    var newSize = this.kt_1;
    return _UIntArray___init__impl__ghjpc6(copyOf_3(_UIntArray___get_storage__impl__92a0v0(tmp0), newSize));
  };
  protoOf(UIntArrayBuilder).co = function () {
    return new UIntArray(this.hv());
  };
  function ShortArrayBuilder_init_$Init$(bufferWithData, $this) {
    ShortArrayBuilder.call($this, bufferWithData, bufferWithData.length);
    return $this;
  }
  function ShortArrayBuilder_init_$Create$(bufferWithData) {
    return ShortArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(ShortArrayBuilder)));
  }
  function ShortArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    ShortArrayBuilder.call($this, new Int16Array(initialCapacity), 0);
    return $this;
  }
  function ShortArrayBuilder_init_$Create$_0(initialCapacity) {
    return ShortArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(ShortArrayBuilder)));
  }
  function ShortArrayBuilder_init_$Init$_1($this) {
    ShortArrayBuilder.call($this, new Int16Array(10), 0);
    return $this;
  }
  function ShortArrayBuilder_init_$Create$_1() {
    return ShortArrayBuilder_init_$Init$_1(objectCreate(protoOf(ShortArrayBuilder)));
  }
  function ShortArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.st_1 = bufferWithData;
    this.tt_1 = initialPosition;
  }
  protoOf(ShortArrayBuilder).ao = function () {
    return this.tt_1;
  };
  protoOf(ShortArrayBuilder).ko = function (requiredCapacity) {
    if (this.st_1.length < requiredCapacity)
      this.st_1 = copyOf_4(this.st_1, coerceAtLeast(requiredCapacity, imul(this.st_1.length, 2)));
  };
  protoOf(ShortArrayBuilder).ut = function (c) {
    this.lo();
    var tmp = this.st_1;
    var _unary__edvuaz = this.tt_1;
    this.tt_1 = _unary__edvuaz + 1 | 0;
    tmp[_unary__edvuaz] = c;
  };
  protoOf(ShortArrayBuilder).co = function () {
    return copyOf_4(this.st_1, this.tt_1);
  };
  function UShortArrayBuilder_init_$Init$(bufferWithData, $this) {
    UShortArrayBuilder.call($this, bufferWithData, _UShortArray___get_size__impl__jqto1b(bufferWithData));
    return $this;
  }
  function UShortArrayBuilder_init_$Create$(bufferWithData) {
    return UShortArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(UShortArrayBuilder)));
  }
  function UShortArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    UShortArrayBuilder.call($this, _UShortArray___init__impl__9b26ef_0(initialCapacity), 0);
    return $this;
  }
  function UShortArrayBuilder_init_$Create$_0(initialCapacity) {
    return UShortArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(UShortArrayBuilder)));
  }
  function UShortArrayBuilder_init_$Init$_1($this) {
    UShortArrayBuilder.call($this, _UShortArray___init__impl__9b26ef_0(10), 0);
    return $this;
  }
  function UShortArrayBuilder_init_$Create$_1() {
    return UShortArrayBuilder_init_$Init$_1(objectCreate(protoOf(UShortArrayBuilder)));
  }
  function UShortArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.bu_1 = bufferWithData;
    this.cu_1 = initialPosition;
  }
  protoOf(UShortArrayBuilder).ao = function () {
    return this.cu_1;
  };
  protoOf(UShortArrayBuilder).ko = function (requiredCapacity) {
    if (_UShortArray___get_size__impl__jqto1b(this.bu_1) < requiredCapacity) {
      var tmp = this;
      var tmp0 = this.bu_1;
      // Inline function 'kotlin.collections.copyOf' call
      var newSize = coerceAtLeast(requiredCapacity, imul(_UShortArray___get_size__impl__jqto1b(this.bu_1), 2));
      tmp.bu_1 = _UShortArray___init__impl__9b26ef(copyOf_4(_UShortArray___get_storage__impl__t2jpv5(tmp0), newSize));
    }
  };
  protoOf(UShortArrayBuilder).du = function (c) {
    this.lo();
    var tmp = this.bu_1;
    var _unary__edvuaz = this.cu_1;
    this.cu_1 = _unary__edvuaz + 1 | 0;
    UShortArray__set_impl_6d8whp(tmp, _unary__edvuaz, c);
  };
  protoOf(UShortArrayBuilder).iv = function () {
    var tmp0 = this.bu_1;
    // Inline function 'kotlin.collections.copyOf' call
    var newSize = this.cu_1;
    return _UShortArray___init__impl__9b26ef(copyOf_4(_UShortArray___get_storage__impl__t2jpv5(tmp0), newSize));
  };
  protoOf(UShortArrayBuilder).co = function () {
    return new UShortArray(this.iv());
  };
  function ByteArrayBuilder_init_$Init$(bufferWithData, $this) {
    ByteArrayBuilder.call($this, bufferWithData, bufferWithData.length);
    return $this;
  }
  function ByteArrayBuilder_init_$Create$(bufferWithData) {
    return ByteArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(ByteArrayBuilder)));
  }
  function ByteArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    ByteArrayBuilder.call($this, new Int8Array(initialCapacity), 0);
    return $this;
  }
  function ByteArrayBuilder_init_$Create$_0(initialCapacity) {
    return ByteArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(ByteArrayBuilder)));
  }
  function ByteArrayBuilder_init_$Init$_1($this) {
    ByteArrayBuilder.call($this, new Int8Array(10), 0);
    return $this;
  }
  function ByteArrayBuilder_init_$Create$_1() {
    return ByteArrayBuilder_init_$Init$_1(objectCreate(protoOf(ByteArrayBuilder)));
  }
  function ByteArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.ku_1 = bufferWithData;
    this.lu_1 = initialPosition;
  }
  protoOf(ByteArrayBuilder).ao = function () {
    return this.lu_1;
  };
  protoOf(ByteArrayBuilder).ko = function (requiredCapacity) {
    if (this.ku_1.length < requiredCapacity)
      this.ku_1 = copyOf_5(this.ku_1, coerceAtLeast(requiredCapacity, imul(this.ku_1.length, 2)));
  };
  protoOf(ByteArrayBuilder).mu = function (c) {
    this.lo();
    var tmp = this.ku_1;
    var _unary__edvuaz = this.lu_1;
    this.lu_1 = _unary__edvuaz + 1 | 0;
    tmp[_unary__edvuaz] = c;
  };
  protoOf(ByteArrayBuilder).co = function () {
    return copyOf_5(this.ku_1, this.lu_1);
  };
  function UByteArrayBuilder_init_$Init$(bufferWithData, $this) {
    UByteArrayBuilder.call($this, bufferWithData, _UByteArray___get_size__impl__h6pkdv(bufferWithData));
    return $this;
  }
  function UByteArrayBuilder_init_$Create$(bufferWithData) {
    return UByteArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(UByteArrayBuilder)));
  }
  function UByteArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    UByteArrayBuilder.call($this, _UByteArray___init__impl__ip4y9n_0(initialCapacity), 0);
    return $this;
  }
  function UByteArrayBuilder_init_$Create$_0(initialCapacity) {
    return UByteArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(UByteArrayBuilder)));
  }
  function UByteArrayBuilder_init_$Init$_1($this) {
    UByteArrayBuilder.call($this, _UByteArray___init__impl__ip4y9n_0(10), 0);
    return $this;
  }
  function UByteArrayBuilder_init_$Create$_1() {
    return UByteArrayBuilder_init_$Init$_1(objectCreate(protoOf(UByteArrayBuilder)));
  }
  function UByteArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.tu_1 = bufferWithData;
    this.uu_1 = initialPosition;
  }
  protoOf(UByteArrayBuilder).ao = function () {
    return this.uu_1;
  };
  protoOf(UByteArrayBuilder).ko = function (requiredCapacity) {
    if (_UByteArray___get_size__impl__h6pkdv(this.tu_1) < requiredCapacity) {
      var tmp = this;
      var tmp0 = this.tu_1;
      // Inline function 'kotlin.collections.copyOf' call
      var newSize = coerceAtLeast(requiredCapacity, imul(_UByteArray___get_size__impl__h6pkdv(this.tu_1), 2));
      tmp.tu_1 = _UByteArray___init__impl__ip4y9n(copyOf_5(_UByteArray___get_storage__impl__d4kctt(tmp0), newSize));
    }
  };
  protoOf(UByteArrayBuilder).vu = function (c) {
    this.lo();
    var tmp = this.tu_1;
    var _unary__edvuaz = this.uu_1;
    this.uu_1 = _unary__edvuaz + 1 | 0;
    UByteArray__set_impl_jvcicn(tmp, _unary__edvuaz, c);
  };
  protoOf(UByteArrayBuilder).jv = function () {
    var tmp0 = this.tu_1;
    // Inline function 'kotlin.collections.copyOf' call
    var newSize = this.uu_1;
    return _UByteArray___init__impl__ip4y9n(copyOf_5(_UByteArray___get_storage__impl__d4kctt(tmp0), newSize));
  };
  protoOf(UByteArrayBuilder).co = function () {
    return new UByteArray(this.jv());
  };
  function BooleanArrayBuilder_init_$Init$(bufferWithData, $this) {
    BooleanArrayBuilder.call($this, bufferWithData, bufferWithData.length);
    return $this;
  }
  function BooleanArrayBuilder_init_$Create$(bufferWithData) {
    return BooleanArrayBuilder_init_$Init$(bufferWithData, objectCreate(protoOf(BooleanArrayBuilder)));
  }
  function BooleanArrayBuilder_init_$Init$_0(initialCapacity, $this) {
    BooleanArrayBuilder.call($this, booleanArray(initialCapacity), 0);
    return $this;
  }
  function BooleanArrayBuilder_init_$Create$_0(initialCapacity) {
    return BooleanArrayBuilder_init_$Init$_0(initialCapacity, objectCreate(protoOf(BooleanArrayBuilder)));
  }
  function BooleanArrayBuilder_init_$Init$_1($this) {
    BooleanArrayBuilder.call($this, booleanArray(10), 0);
    return $this;
  }
  function BooleanArrayBuilder_init_$Create$_1() {
    return BooleanArrayBuilder_init_$Init$_1(objectCreate(protoOf(BooleanArrayBuilder)));
  }
  function BooleanArrayBuilder(bufferWithData, initialPosition) {
    PrimitiveArrayBuilder.call(this);
    this.cv_1 = bufferWithData;
    this.dv_1 = initialPosition;
  }
  protoOf(BooleanArrayBuilder).ao = function () {
    return this.dv_1;
  };
  protoOf(BooleanArrayBuilder).ko = function (requiredCapacity) {
    if (this.cv_1.length < requiredCapacity)
      this.cv_1 = copyOf_6(this.cv_1, coerceAtLeast(requiredCapacity, imul(this.cv_1.length, 2)));
  };
  protoOf(BooleanArrayBuilder).ev = function (c) {
    this.lo();
    var tmp = this.cv_1;
    var _unary__edvuaz = this.dv_1;
    this.dv_1 = _unary__edvuaz + 1 | 0;
    tmp[_unary__edvuaz] = c;
  };
  protoOf(BooleanArrayBuilder).co = function () {
    return copyOf_6(this.cv_1, this.dv_1);
  };
  function get_BUILTIN_SERIALIZERS() {
    _init_properties_Primitives_kt__k0eto4();
    return BUILTIN_SERIALIZERS;
  }
  var BUILTIN_SERIALIZERS;
  function StringSerializer() {
    StringSerializer_instance = this;
    this.kv_1 = new PrimitiveSerialDescriptor_0('kotlin.String', STRING_getInstance());
  }
  protoOf(StringSerializer).of = function () {
    return this.kv_1;
  };
  protoOf(StringSerializer).lv = function (encoder, value) {
    return encoder.uj(value);
  };
  protoOf(StringSerializer).pf = function (encoder, value) {
    return this.lv(encoder, (!(value == null) ? typeof value === 'string' : false) ? value : THROW_CCE());
  };
  protoOf(StringSerializer).qf = function (decoder) {
    return decoder.li();
  };
  var StringSerializer_instance;
  function StringSerializer_getInstance() {
    if (StringSerializer_instance == null)
      new StringSerializer();
    return StringSerializer_instance;
  }
  function error_0($this) {
    throw IllegalStateException_init_$Create$('Primitive descriptor ' + $this.mv_1 + ' does not have elements');
  }
  function PrimitiveSerialDescriptor_0(serialName, kind) {
    this.mv_1 = serialName;
    this.nv_1 = kind;
  }
  protoOf(PrimitiveSerialDescriptor_0).tg = function () {
    return this.mv_1;
  };
  protoOf(PrimitiveSerialDescriptor_0).ug = function () {
    return this.nv_1;
  };
  protoOf(PrimitiveSerialDescriptor_0).wg = function () {
    return 0;
  };
  protoOf(PrimitiveSerialDescriptor_0).yg = function (index) {
    error_0(this);
  };
  protoOf(PrimitiveSerialDescriptor_0).zg = function (name) {
    error_0(this);
  };
  protoOf(PrimitiveSerialDescriptor_0).ch = function (index) {
    error_0(this);
  };
  protoOf(PrimitiveSerialDescriptor_0).bh = function (index) {
    error_0(this);
  };
  protoOf(PrimitiveSerialDescriptor_0).ah = function (index) {
    error_0(this);
  };
  protoOf(PrimitiveSerialDescriptor_0).toString = function () {
    return 'PrimitiveDescriptor(' + this.mv_1 + ')';
  };
  protoOf(PrimitiveSerialDescriptor_0).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof PrimitiveSerialDescriptor_0))
      return false;
    if (this.mv_1 === other.mv_1 && equals(this.nv_1, other.nv_1))
      return true;
    return false;
  };
  protoOf(PrimitiveSerialDescriptor_0).hashCode = function () {
    return getStringHashCode(this.mv_1) + imul(31, this.nv_1.hashCode()) | 0;
  };
  function builtinSerializerOrNull(_this__u8e3s4) {
    _init_properties_Primitives_kt__k0eto4();
    var tmp = get_BUILTIN_SERIALIZERS().b2(_this__u8e3s4);
    return (tmp == null ? true : isInterface(tmp, KSerializer)) ? tmp : THROW_CCE();
  }
  function IntSerializer() {
    IntSerializer_instance = this;
    this.ov_1 = new PrimitiveSerialDescriptor_0('kotlin.Int', INT_getInstance());
  }
  protoOf(IntSerializer).of = function () {
    return this.ov_1;
  };
  protoOf(IntSerializer).pv = function (encoder, value) {
    return encoder.pj(value);
  };
  protoOf(IntSerializer).pf = function (encoder, value) {
    return this.pv(encoder, (!(value == null) ? typeof value === 'number' : false) ? value : THROW_CCE());
  };
  protoOf(IntSerializer).qf = function (decoder) {
    return decoder.gi();
  };
  var IntSerializer_instance;
  function IntSerializer_getInstance() {
    if (IntSerializer_instance == null)
      new IntSerializer();
    return IntSerializer_instance;
  }
  function LongSerializer() {
    LongSerializer_instance = this;
    this.qv_1 = new PrimitiveSerialDescriptor_0('kotlin.Long', LONG_getInstance());
  }
  protoOf(LongSerializer).of = function () {
    return this.qv_1;
  };
  protoOf(LongSerializer).rv = function (encoder, value) {
    return encoder.qj(value);
  };
  protoOf(LongSerializer).pf = function (encoder, value) {
    return this.rv(encoder, value instanceof Long ? value : THROW_CCE());
  };
  protoOf(LongSerializer).qf = function (decoder) {
    return decoder.hi();
  };
  var LongSerializer_instance;
  function LongSerializer_getInstance() {
    if (LongSerializer_instance == null)
      new LongSerializer();
    return LongSerializer_instance;
  }
  function ByteSerializer() {
    ByteSerializer_instance = this;
    this.sv_1 = new PrimitiveSerialDescriptor_0('kotlin.Byte', BYTE_getInstance());
  }
  protoOf(ByteSerializer).of = function () {
    return this.sv_1;
  };
  protoOf(ByteSerializer).tv = function (encoder, value) {
    return encoder.nj(value);
  };
  protoOf(ByteSerializer).pf = function (encoder, value) {
    return this.tv(encoder, (!(value == null) ? typeof value === 'number' : false) ? value : THROW_CCE());
  };
  protoOf(ByteSerializer).qf = function (decoder) {
    return decoder.ei();
  };
  var ByteSerializer_instance;
  function ByteSerializer_getInstance() {
    if (ByteSerializer_instance == null)
      new ByteSerializer();
    return ByteSerializer_instance;
  }
  function ShortSerializer() {
    ShortSerializer_instance = this;
    this.uv_1 = new PrimitiveSerialDescriptor_0('kotlin.Short', SHORT_getInstance());
  }
  protoOf(ShortSerializer).of = function () {
    return this.uv_1;
  };
  protoOf(ShortSerializer).vv = function (encoder, value) {
    return encoder.oj(value);
  };
  protoOf(ShortSerializer).pf = function (encoder, value) {
    return this.vv(encoder, (!(value == null) ? typeof value === 'number' : false) ? value : THROW_CCE());
  };
  protoOf(ShortSerializer).qf = function (decoder) {
    return decoder.fi();
  };
  var ShortSerializer_instance;
  function ShortSerializer_getInstance() {
    if (ShortSerializer_instance == null)
      new ShortSerializer();
    return ShortSerializer_instance;
  }
  function CharSerializer() {
    CharSerializer_instance = this;
    this.wv_1 = new PrimitiveSerialDescriptor_0('kotlin.Char', CHAR_getInstance());
  }
  protoOf(CharSerializer).of = function () {
    return this.wv_1;
  };
  protoOf(CharSerializer).xv = function (encoder, value) {
    return encoder.tj(value);
  };
  protoOf(CharSerializer).pf = function (encoder, value) {
    return this.xv(encoder, value instanceof Char ? value.j1_1 : THROW_CCE());
  };
  protoOf(CharSerializer).yv = function (decoder) {
    return decoder.ki();
  };
  protoOf(CharSerializer).qf = function (decoder) {
    return new Char(this.yv(decoder));
  };
  var CharSerializer_instance;
  function CharSerializer_getInstance() {
    if (CharSerializer_instance == null)
      new CharSerializer();
    return CharSerializer_instance;
  }
  function DoubleSerializer() {
    DoubleSerializer_instance = this;
    this.zv_1 = new PrimitiveSerialDescriptor_0('kotlin.Double', DOUBLE_getInstance());
  }
  protoOf(DoubleSerializer).of = function () {
    return this.zv_1;
  };
  protoOf(DoubleSerializer).aw = function (encoder, value) {
    return encoder.sj(value);
  };
  protoOf(DoubleSerializer).pf = function (encoder, value) {
    return this.aw(encoder, (!(value == null) ? typeof value === 'number' : false) ? value : THROW_CCE());
  };
  protoOf(DoubleSerializer).qf = function (decoder) {
    return decoder.ji();
  };
  var DoubleSerializer_instance;
  function DoubleSerializer_getInstance() {
    if (DoubleSerializer_instance == null)
      new DoubleSerializer();
    return DoubleSerializer_instance;
  }
  function FloatSerializer() {
    FloatSerializer_instance = this;
    this.bw_1 = new PrimitiveSerialDescriptor_0('kotlin.Float', FLOAT_getInstance());
  }
  protoOf(FloatSerializer).of = function () {
    return this.bw_1;
  };
  protoOf(FloatSerializer).cw = function (encoder, value) {
    return encoder.rj(value);
  };
  protoOf(FloatSerializer).pf = function (encoder, value) {
    return this.cw(encoder, (!(value == null) ? typeof value === 'number' : false) ? value : THROW_CCE());
  };
  protoOf(FloatSerializer).qf = function (decoder) {
    return decoder.ii();
  };
  var FloatSerializer_instance;
  function FloatSerializer_getInstance() {
    if (FloatSerializer_instance == null)
      new FloatSerializer();
    return FloatSerializer_instance;
  }
  function BooleanSerializer() {
    BooleanSerializer_instance = this;
    this.dw_1 = new PrimitiveSerialDescriptor_0('kotlin.Boolean', BOOLEAN_getInstance());
  }
  protoOf(BooleanSerializer).of = function () {
    return this.dw_1;
  };
  protoOf(BooleanSerializer).ew = function (encoder, value) {
    return encoder.mj(value);
  };
  protoOf(BooleanSerializer).pf = function (encoder, value) {
    return this.ew(encoder, (!(value == null) ? typeof value === 'boolean' : false) ? value : THROW_CCE());
  };
  protoOf(BooleanSerializer).qf = function (decoder) {
    return decoder.di();
  };
  var BooleanSerializer_instance;
  function BooleanSerializer_getInstance() {
    if (BooleanSerializer_instance == null)
      new BooleanSerializer();
    return BooleanSerializer_instance;
  }
  function UnitSerializer() {
    UnitSerializer_instance = this;
    this.fw_1 = new ObjectSerializer('kotlin.Unit', Unit_instance);
  }
  protoOf(UnitSerializer).of = function () {
    return this.fw_1.of();
  };
  protoOf(UnitSerializer).gw = function (encoder, value) {
    this.fw_1.eg(encoder, Unit_instance);
  };
  protoOf(UnitSerializer).pf = function (encoder, value) {
    return this.gw(encoder, value instanceof Unit ? value : THROW_CCE());
  };
  protoOf(UnitSerializer).hw = function (decoder) {
    this.fw_1.qf(decoder);
  };
  protoOf(UnitSerializer).qf = function (decoder) {
    this.hw(decoder);
    return Unit_instance;
  };
  var UnitSerializer_instance;
  function UnitSerializer_getInstance() {
    if (UnitSerializer_instance == null)
      new UnitSerializer();
    return UnitSerializer_instance;
  }
  function PrimitiveDescriptorSafe(serialName, kind) {
    _init_properties_Primitives_kt__k0eto4();
    checkNameIsNotAPrimitive(serialName);
    return new PrimitiveSerialDescriptor_0(serialName, kind);
  }
  function checkNameIsNotAPrimitive(serialName) {
    _init_properties_Primitives_kt__k0eto4();
    var values = get_BUILTIN_SERIALIZERS().d2();
    var _iterator__ex2g4s = values.r();
    while (_iterator__ex2g4s.s()) {
      var primitive = _iterator__ex2g4s.t();
      var primitiveName = primitive.of().tg();
      if (serialName === primitiveName) {
        throw IllegalArgumentException_init_$Create$(trimIndent('\n                The name of serial descriptor should uniquely identify associated serializer.\n                For serial name ' + serialName + ' there already exists ' + getKClassFromExpression(primitive).m() + '.\n                Please refer to SerialDescriptor documentation for additional information.\n            '));
      }
    }
  }
  var properties_initialized_Primitives_kt_6dpii6;
  function _init_properties_Primitives_kt__k0eto4() {
    if (!properties_initialized_Primitives_kt_6dpii6) {
      properties_initialized_Primitives_kt_6dpii6 = true;
      BUILTIN_SERIALIZERS = initBuiltins();
    }
  }
  function NamedValueDecoder() {
    TaggedDecoder.call(this);
  }
  protoOf(NamedValueDecoder).kw = function (_this__u8e3s4, index) {
    return this.mw(this.lw(_this__u8e3s4, index));
  };
  protoOf(NamedValueDecoder).mw = function (nestedName) {
    var tmp0_elvis_lhs = this.pw();
    return this.qw(tmp0_elvis_lhs == null ? '' : tmp0_elvis_lhs, nestedName);
  };
  protoOf(NamedValueDecoder).lw = function (descriptor, index) {
    return descriptor.yg(index);
  };
  protoOf(NamedValueDecoder).qw = function (parentName, childName) {
    var tmp;
    // Inline function 'kotlin.text.isEmpty' call
    if (charSequenceLength(parentName) === 0) {
      tmp = childName;
    } else {
      tmp = parentName + '.' + childName;
    }
    return tmp;
  };
  protoOf(NamedValueDecoder).rw = function () {
    return this.nw_1.p() ? '$' : joinToString(this.nw_1, '.', '$.');
  };
  function tagBlock($this, tag, block) {
    $this.fx(tag);
    var r = block();
    if (!$this.ow_1) {
      $this.gx();
    }
    $this.ow_1 = false;
    return r;
  }
  function TaggedDecoder$decodeSerializableElement$lambda(this$0, $deserializer, $previousValue) {
    return function () {
      return this$0.oi($deserializer, $previousValue);
    };
  }
  function TaggedDecoder$decodeNullableSerializableElement$lambda(this$0, $deserializer, $previousValue) {
    return function () {
      var tmp0 = this$0;
      // Inline function 'kotlinx.serialization.encoding.decodeIfNullable' call
      var isNullabilitySupported = $deserializer.of().pg();
      var tmp;
      if (isNullabilitySupported || tmp0.bi()) {
        tmp = this$0.oi($deserializer, $previousValue);
      } else {
        tmp = tmp0.ci();
      }
      return tmp;
    };
  }
  function TaggedDecoder() {
    var tmp = this;
    // Inline function 'kotlin.collections.arrayListOf' call
    tmp.nw_1 = ArrayList_init_$Create$_0();
    this.ow_1 = false;
  }
  protoOf(TaggedDecoder).fj = function () {
    return EmptySerializersModule_0();
  };
  protoOf(TaggedDecoder).sw = function (tag) {
    throw SerializationException_init_$Create$_0(toString(getKClassFromExpression(this)) + " can't retrieve untyped values");
  };
  protoOf(TaggedDecoder).tw = function (tag) {
    return true;
  };
  protoOf(TaggedDecoder).uw = function (tag) {
    var tmp = this.sw(tag);
    return typeof tmp === 'boolean' ? tmp : THROW_CCE();
  };
  protoOf(TaggedDecoder).vw = function (tag) {
    var tmp = this.sw(tag);
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(TaggedDecoder).ww = function (tag) {
    var tmp = this.sw(tag);
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(TaggedDecoder).xw = function (tag) {
    var tmp = this.sw(tag);
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(TaggedDecoder).yw = function (tag) {
    var tmp = this.sw(tag);
    return tmp instanceof Long ? tmp : THROW_CCE();
  };
  protoOf(TaggedDecoder).zw = function (tag) {
    var tmp = this.sw(tag);
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(TaggedDecoder).ax = function (tag) {
    var tmp = this.sw(tag);
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(TaggedDecoder).bx = function (tag) {
    var tmp = this.sw(tag);
    return tmp instanceof Char ? tmp.j1_1 : THROW_CCE();
  };
  protoOf(TaggedDecoder).cx = function (tag) {
    var tmp = this.sw(tag);
    return typeof tmp === 'string' ? tmp : THROW_CCE();
  };
  protoOf(TaggedDecoder).dx = function (tag, enumDescriptor) {
    var tmp = this.sw(tag);
    return typeof tmp === 'number' ? tmp : THROW_CCE();
  };
  protoOf(TaggedDecoder).ex = function (tag, inlineDescriptor) {
    // Inline function 'kotlin.apply' call
    this.fx(tag);
    return this;
  };
  protoOf(TaggedDecoder).oi = function (deserializer, previousValue) {
    return this.pi(deserializer);
  };
  protoOf(TaggedDecoder).ni = function (descriptor) {
    return this.ex(this.gx(), descriptor);
  };
  protoOf(TaggedDecoder).bi = function () {
    var tmp0_elvis_lhs = this.pw();
    var tmp;
    if (tmp0_elvis_lhs == null) {
      return false;
    } else {
      tmp = tmp0_elvis_lhs;
    }
    var currentTag = tmp;
    return this.tw(currentTag);
  };
  protoOf(TaggedDecoder).ci = function () {
    return null;
  };
  protoOf(TaggedDecoder).di = function () {
    return this.uw(this.gx());
  };
  protoOf(TaggedDecoder).ei = function () {
    return this.vw(this.gx());
  };
  protoOf(TaggedDecoder).fi = function () {
    return this.ww(this.gx());
  };
  protoOf(TaggedDecoder).gi = function () {
    return this.xw(this.gx());
  };
  protoOf(TaggedDecoder).hi = function () {
    return this.yw(this.gx());
  };
  protoOf(TaggedDecoder).ii = function () {
    return this.zw(this.gx());
  };
  protoOf(TaggedDecoder).ji = function () {
    return this.ax(this.gx());
  };
  protoOf(TaggedDecoder).ki = function () {
    return this.bx(this.gx());
  };
  protoOf(TaggedDecoder).li = function () {
    return this.cx(this.gx());
  };
  protoOf(TaggedDecoder).mi = function (enumDescriptor) {
    return this.dx(this.gx(), enumDescriptor);
  };
  protoOf(TaggedDecoder).qi = function (descriptor) {
    return this;
  };
  protoOf(TaggedDecoder).ri = function (descriptor) {
  };
  protoOf(TaggedDecoder).si = function (descriptor, index) {
    return this.uw(this.kw(descriptor, index));
  };
  protoOf(TaggedDecoder).ti = function (descriptor, index) {
    return this.vw(this.kw(descriptor, index));
  };
  protoOf(TaggedDecoder).ui = function (descriptor, index) {
    return this.ww(this.kw(descriptor, index));
  };
  protoOf(TaggedDecoder).vi = function (descriptor, index) {
    return this.xw(this.kw(descriptor, index));
  };
  protoOf(TaggedDecoder).wi = function (descriptor, index) {
    return this.yw(this.kw(descriptor, index));
  };
  protoOf(TaggedDecoder).xi = function (descriptor, index) {
    return this.zw(this.kw(descriptor, index));
  };
  protoOf(TaggedDecoder).yi = function (descriptor, index) {
    return this.ax(this.kw(descriptor, index));
  };
  protoOf(TaggedDecoder).zi = function (descriptor, index) {
    return this.bx(this.kw(descriptor, index));
  };
  protoOf(TaggedDecoder).aj = function (descriptor, index) {
    return this.cx(this.kw(descriptor, index));
  };
  protoOf(TaggedDecoder).bj = function (descriptor, index) {
    return this.ex(this.kw(descriptor, index), descriptor.bh(index));
  };
  protoOf(TaggedDecoder).cj = function (descriptor, index, deserializer, previousValue) {
    var tmp = this.kw(descriptor, index);
    return tagBlock(this, tmp, TaggedDecoder$decodeSerializableElement$lambda(this, deserializer, previousValue));
  };
  protoOf(TaggedDecoder).ej = function (descriptor, index, deserializer, previousValue) {
    var tmp = this.kw(descriptor, index);
    return tagBlock(this, tmp, TaggedDecoder$decodeNullableSerializableElement$lambda(this, deserializer, previousValue));
  };
  protoOf(TaggedDecoder).pw = function () {
    return lastOrNull(this.nw_1);
  };
  protoOf(TaggedDecoder).fx = function (name) {
    this.nw_1.w(name);
  };
  protoOf(TaggedDecoder).gx = function () {
    var r = this.nw_1.g2(get_lastIndex_0(this.nw_1));
    this.ow_1 = true;
    return r;
  };
  function get_NULL() {
    _init_properties_Tuples_kt__dz0qyd();
    return NULL;
  }
  var NULL;
  function MapEntry(key, value) {
    this.hx_1 = key;
    this.ix_1 = value;
  }
  protoOf(MapEntry).x1 = function () {
    return this.hx_1;
  };
  protoOf(MapEntry).y1 = function () {
    return this.ix_1;
  };
  protoOf(MapEntry).toString = function () {
    return 'MapEntry(key=' + toString_0(this.hx_1) + ', value=' + toString_0(this.ix_1) + ')';
  };
  protoOf(MapEntry).hashCode = function () {
    var result = this.hx_1 == null ? 0 : hashCode(this.hx_1);
    result = imul(result, 31) + (this.ix_1 == null ? 0 : hashCode(this.ix_1)) | 0;
    return result;
  };
  protoOf(MapEntry).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof MapEntry))
      return false;
    if (!equals(this.hx_1, other.hx_1))
      return false;
    if (!equals(this.ix_1, other.ix_1))
      return false;
    return true;
  };
  function MapEntrySerializer$descriptor$lambda($keySerializer, $valueSerializer) {
    return function ($this$buildSerialDescriptor) {
      $this$buildSerialDescriptor.zf('key', $keySerializer.of());
      $this$buildSerialDescriptor.zf('value', $valueSerializer.of());
      return Unit_instance;
    };
  }
  function MapEntrySerializer_0(keySerializer, valueSerializer) {
    KeyValueSerializer.call(this, keySerializer, valueSerializer);
    var tmp = this;
    var tmp_0 = MAP_getInstance();
    tmp.lx_1 = buildSerialDescriptor('kotlin.collections.Map.Entry', tmp_0, [], MapEntrySerializer$descriptor$lambda(keySerializer, valueSerializer));
  }
  protoOf(MapEntrySerializer_0).of = function () {
    return this.lx_1;
  };
  protoOf(MapEntrySerializer_0).mx = function (_this__u8e3s4) {
    return _this__u8e3s4.x1();
  };
  protoOf(MapEntrySerializer_0).nx = function (_this__u8e3s4) {
    return this.mx((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, Entry) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(MapEntrySerializer_0).ox = function (_this__u8e3s4) {
    return _this__u8e3s4.y1();
  };
  protoOf(MapEntrySerializer_0).px = function (_this__u8e3s4) {
    return this.ox((!(_this__u8e3s4 == null) ? isInterface(_this__u8e3s4, Entry) : false) ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(MapEntrySerializer_0).qx = function (key, value) {
    return new MapEntry(key, value);
  };
  function PairSerializer$descriptor$lambda($keySerializer, $valueSerializer) {
    return function ($this$buildClassSerialDescriptor) {
      $this$buildClassSerialDescriptor.zf('first', $keySerializer.of());
      $this$buildClassSerialDescriptor.zf('second', $valueSerializer.of());
      return Unit_instance;
    };
  }
  function PairSerializer_0(keySerializer, valueSerializer) {
    KeyValueSerializer.call(this, keySerializer, valueSerializer);
    var tmp = this;
    tmp.wx_1 = buildClassSerialDescriptor('kotlin.Pair', [], PairSerializer$descriptor$lambda(keySerializer, valueSerializer));
  }
  protoOf(PairSerializer_0).of = function () {
    return this.wx_1;
  };
  protoOf(PairSerializer_0).xx = function (_this__u8e3s4) {
    return _this__u8e3s4.ra_1;
  };
  protoOf(PairSerializer_0).nx = function (_this__u8e3s4) {
    return this.xx(_this__u8e3s4 instanceof Pair ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(PairSerializer_0).yx = function (_this__u8e3s4) {
    return _this__u8e3s4.sa_1;
  };
  protoOf(PairSerializer_0).px = function (_this__u8e3s4) {
    return this.yx(_this__u8e3s4 instanceof Pair ? _this__u8e3s4 : THROW_CCE());
  };
  protoOf(PairSerializer_0).qx = function (key, value) {
    return to(key, value);
  };
  function decodeSequentially_1($this, composite) {
    var a = composite.dj($this.cy_1, 0, $this.zx_1);
    var b = composite.dj($this.cy_1, 1, $this.ay_1);
    var c = composite.dj($this.cy_1, 2, $this.by_1);
    composite.ri($this.cy_1);
    return new Triple(a, b, c);
  }
  function decodeStructure($this, composite) {
    var a = get_NULL();
    var b = get_NULL();
    var c = get_NULL();
    mainLoop: while (true) {
      var index = composite.hj($this.cy_1);
      switch (index) {
        case -1:
          break mainLoop;
        case 0:
          a = composite.dj($this.cy_1, 0, $this.zx_1);
          break;
        case 1:
          b = composite.dj($this.cy_1, 1, $this.ay_1);
          break;
        case 2:
          c = composite.dj($this.cy_1, 2, $this.by_1);
          break;
        default:
          throw SerializationException_init_$Create$_0('Unexpected index ' + index);
      }
    }
    composite.ri($this.cy_1);
    if (a === get_NULL())
      throw SerializationException_init_$Create$_0("Element 'first' is missing");
    if (b === get_NULL())
      throw SerializationException_init_$Create$_0("Element 'second' is missing");
    if (c === get_NULL())
      throw SerializationException_init_$Create$_0("Element 'third' is missing");
    return new Triple(a, b, c);
  }
  function TripleSerializer$descriptor$lambda(this$0) {
    return function ($this$buildClassSerialDescriptor) {
      $this$buildClassSerialDescriptor.zf('first', this$0.zx_1.of());
      $this$buildClassSerialDescriptor.zf('second', this$0.ay_1.of());
      $this$buildClassSerialDescriptor.zf('third', this$0.by_1.of());
      return Unit_instance;
    };
  }
  function TripleSerializer_0(aSerializer, bSerializer, cSerializer) {
    this.zx_1 = aSerializer;
    this.ay_1 = bSerializer;
    this.by_1 = cSerializer;
    var tmp = this;
    tmp.cy_1 = buildClassSerialDescriptor('kotlin.Triple', [], TripleSerializer$descriptor$lambda(this));
  }
  protoOf(TripleSerializer_0).of = function () {
    return this.cy_1;
  };
  protoOf(TripleSerializer_0).dy = function (encoder, value) {
    var structuredEncoder = encoder.qi(this.cy_1);
    structuredEncoder.hk(this.cy_1, 0, this.zx_1, value.sd_1);
    structuredEncoder.hk(this.cy_1, 1, this.ay_1, value.td_1);
    structuredEncoder.hk(this.cy_1, 2, this.by_1, value.ud_1);
    structuredEncoder.ri(this.cy_1);
  };
  protoOf(TripleSerializer_0).pf = function (encoder, value) {
    return this.dy(encoder, value instanceof Triple ? value : THROW_CCE());
  };
  protoOf(TripleSerializer_0).qf = function (decoder) {
    var composite = decoder.qi(this.cy_1);
    if (composite.gj()) {
      return decodeSequentially_1(this, composite);
    }
    return decodeStructure(this, composite);
  };
  function KeyValueSerializer(keySerializer, valueSerializer) {
    this.rx_1 = keySerializer;
    this.sx_1 = valueSerializer;
  }
  protoOf(KeyValueSerializer).tx = function (encoder, value) {
    var structuredEncoder = encoder.qi(this.of());
    structuredEncoder.hk(this.of(), 0, this.rx_1, this.nx(value));
    structuredEncoder.hk(this.of(), 1, this.sx_1, this.px(value));
    structuredEncoder.ri(this.of());
  };
  protoOf(KeyValueSerializer).pf = function (encoder, value) {
    return this.tx(encoder, value);
  };
  protoOf(KeyValueSerializer).qf = function (decoder) {
    // Inline function 'kotlinx.serialization.encoding.decodeStructure' call
    var descriptor = this.of();
    var composite = decoder.qi(descriptor);
    var tmp$ret$1;
    $l$block: {
      if (composite.gj()) {
        var key = composite.dj(this.of(), 0, this.rx_1);
        var value = composite.dj(this.of(), 1, this.sx_1);
        tmp$ret$1 = this.qx(key, value);
        break $l$block;
      }
      var key_0 = get_NULL();
      var value_0 = get_NULL();
      mainLoop: while (true) {
        var idx = composite.hj(this.of());
        switch (idx) {
          case -1:
            break mainLoop;
          case 0:
            key_0 = composite.dj(this.of(), 0, this.rx_1);
            break;
          case 1:
            value_0 = composite.dj(this.of(), 1, this.sx_1);
            break;
          default:
            throw SerializationException_init_$Create$_0('Invalid index: ' + idx);
        }
      }
      if (key_0 === get_NULL())
        throw SerializationException_init_$Create$_0("Element 'key' is missing");
      if (value_0 === get_NULL())
        throw SerializationException_init_$Create$_0("Element 'value' is missing");
      tmp$ret$1 = this.qx(key_0, value_0);
    }
    var result = tmp$ret$1;
    composite.ri(descriptor);
    return result;
  };
  var properties_initialized_Tuples_kt_3vs7ar;
  function _init_properties_Tuples_kt__dz0qyd() {
    if (!properties_initialized_Tuples_kt_3vs7ar) {
      properties_initialized_Tuples_kt_3vs7ar = true;
      NULL = new Object();
    }
  }
  function UIntSerializer() {
    UIntSerializer_instance = this;
    this.ey_1 = InlinePrimitiveDescriptor('kotlin.UInt', serializer_5(IntCompanionObject_instance));
  }
  protoOf(UIntSerializer).of = function () {
    return this.ey_1;
  };
  protoOf(UIntSerializer).fy = function (encoder, value) {
    var tmp = encoder.wj(this.ey_1);
    // Inline function 'kotlin.UInt.toInt' call
    var tmp$ret$0 = _UInt___get_data__impl__f0vqqw(value);
    tmp.pj(tmp$ret$0);
  };
  protoOf(UIntSerializer).pf = function (encoder, value) {
    return this.fy(encoder, value instanceof UInt ? value.oe_1 : THROW_CCE());
  };
  protoOf(UIntSerializer).gy = function (decoder) {
    // Inline function 'kotlin.toUInt' call
    var this_0 = decoder.ni(this.ey_1).gi();
    return _UInt___init__impl__l7qpdl(this_0);
  };
  protoOf(UIntSerializer).qf = function (decoder) {
    return new UInt(this.gy(decoder));
  };
  var UIntSerializer_instance;
  function UIntSerializer_getInstance() {
    if (UIntSerializer_instance == null)
      new UIntSerializer();
    return UIntSerializer_instance;
  }
  function ULongSerializer() {
    ULongSerializer_instance = this;
    this.hy_1 = InlinePrimitiveDescriptor('kotlin.ULong', serializer_6(Companion_getInstance_3()));
  }
  protoOf(ULongSerializer).of = function () {
    return this.hy_1;
  };
  protoOf(ULongSerializer).iy = function (encoder, value) {
    var tmp = encoder.wj(this.hy_1);
    // Inline function 'kotlin.ULong.toLong' call
    var tmp$ret$0 = _ULong___get_data__impl__fggpzb(value);
    tmp.qj(tmp$ret$0);
  };
  protoOf(ULongSerializer).pf = function (encoder, value) {
    return this.iy(encoder, value instanceof ULong ? value.ye_1 : THROW_CCE());
  };
  protoOf(ULongSerializer).jy = function (decoder) {
    // Inline function 'kotlin.toULong' call
    var this_0 = decoder.ni(this.hy_1).hi();
    return _ULong___init__impl__c78o9k(this_0);
  };
  protoOf(ULongSerializer).qf = function (decoder) {
    return new ULong(this.jy(decoder));
  };
  var ULongSerializer_instance;
  function ULongSerializer_getInstance() {
    if (ULongSerializer_instance == null)
      new ULongSerializer();
    return ULongSerializer_instance;
  }
  function UByteSerializer() {
    UByteSerializer_instance = this;
    this.ky_1 = InlinePrimitiveDescriptor('kotlin.UByte', serializer_7(ByteCompanionObject_instance));
  }
  protoOf(UByteSerializer).of = function () {
    return this.ky_1;
  };
  protoOf(UByteSerializer).ly = function (encoder, value) {
    var tmp = encoder.wj(this.ky_1);
    // Inline function 'kotlin.UByte.toByte' call
    var tmp$ret$0 = _UByte___get_data__impl__jof9qr(value);
    tmp.nj(tmp$ret$0);
  };
  protoOf(UByteSerializer).pf = function (encoder, value) {
    return this.ly(encoder, value instanceof UByte ? value.ee_1 : THROW_CCE());
  };
  protoOf(UByteSerializer).my = function (decoder) {
    // Inline function 'kotlin.toUByte' call
    var this_0 = decoder.ni(this.ky_1).ei();
    return _UByte___init__impl__g9hnc4(this_0);
  };
  protoOf(UByteSerializer).qf = function (decoder) {
    return new UByte(this.my(decoder));
  };
  var UByteSerializer_instance;
  function UByteSerializer_getInstance() {
    if (UByteSerializer_instance == null)
      new UByteSerializer();
    return UByteSerializer_instance;
  }
  function UShortSerializer() {
    UShortSerializer_instance = this;
    this.ny_1 = InlinePrimitiveDescriptor('kotlin.UShort', serializer_8(ShortCompanionObject_instance));
  }
  protoOf(UShortSerializer).of = function () {
    return this.ny_1;
  };
  protoOf(UShortSerializer).oy = function (encoder, value) {
    var tmp = encoder.wj(this.ny_1);
    // Inline function 'kotlin.UShort.toShort' call
    var tmp$ret$0 = _UShort___get_data__impl__g0245(value);
    tmp.oj(tmp$ret$0);
  };
  protoOf(UShortSerializer).pf = function (encoder, value) {
    return this.oy(encoder, value instanceof UShort ? value.if_1 : THROW_CCE());
  };
  protoOf(UShortSerializer).py = function (decoder) {
    // Inline function 'kotlin.toUShort' call
    var this_0 = decoder.ni(this.ny_1).fi();
    return _UShort___init__impl__jigrne(this_0);
  };
  protoOf(UShortSerializer).qf = function (decoder) {
    return new UShort(this.py(decoder));
  };
  var UShortSerializer_instance;
  function UShortSerializer_getInstance() {
    if (UShortSerializer_instance == null)
      new UShortSerializer();
    return UShortSerializer_instance;
  }
  function get_EmptySerializersModuleLegacyJs() {
    _init_properties_SerializersModule_kt__u78ha3();
    return EmptySerializersModule;
  }
  var EmptySerializersModule;
  function SerializersModule() {
  }
  protoOf(SerializersModule).lg = function (kClass, typeArgumentsSerializers, $super) {
    typeArgumentsSerializers = typeArgumentsSerializers === VOID ? emptyList() : typeArgumentsSerializers;
    return $super === VOID ? this.mg(kClass, typeArgumentsSerializers) : $super.mg.call(this, kClass, typeArgumentsSerializers);
  };
  function SerialModuleImpl(class2ContextualFactory, polyBase2Serializers, polyBase2DefaultSerializerProvider, polyBase2NamedSerializers, polyBase2DefaultDeserializerProvider, hasInterfaceContextualSerializers) {
    SerializersModule.call(this);
    this.ry_1 = class2ContextualFactory;
    this.sy_1 = polyBase2Serializers;
    this.ty_1 = polyBase2DefaultSerializerProvider;
    this.uy_1 = polyBase2NamedSerializers;
    this.vy_1 = polyBase2DefaultDeserializerProvider;
    this.wy_1 = hasInterfaceContextualSerializers;
  }
  protoOf(SerialModuleImpl).kg = function () {
    return this.wy_1;
  };
  protoOf(SerialModuleImpl).pk = function (baseClass, value) {
    if (!baseClass.g8(value))
      return null;
    var tmp0_safe_receiver = this.sy_1.b2(baseClass);
    var tmp = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.b2(getKClassFromExpression(value));
    var registered = (!(tmp == null) ? isInterface(tmp, SerializationStrategy) : false) ? tmp : null;
    if (!(registered == null))
      return registered;
    var tmp_0 = this.ty_1.b2(baseClass);
    var tmp1_safe_receiver = (!(tmp_0 == null) ? typeof tmp_0 === 'function' : false) ? tmp_0 : null;
    return tmp1_safe_receiver == null ? null : tmp1_safe_receiver(value);
  };
  protoOf(SerialModuleImpl).ok = function (baseClass, serializedClassName) {
    var tmp0_safe_receiver = this.uy_1.b2(baseClass);
    var tmp;
    if (tmp0_safe_receiver == null) {
      tmp = null;
    } else {
      // Inline function 'kotlin.collections.get' call
      tmp = (isInterface(tmp0_safe_receiver, KtMap) ? tmp0_safe_receiver : THROW_CCE()).b2(serializedClassName);
    }
    var tmp_0 = tmp;
    var registered = (!(tmp_0 == null) ? isInterface(tmp_0, KSerializer) : false) ? tmp_0 : null;
    if (!(registered == null))
      return registered;
    var tmp_1 = this.vy_1.b2(baseClass);
    var tmp1_safe_receiver = (!(tmp_1 == null) ? typeof tmp_1 === 'function' : false) ? tmp_1 : null;
    return tmp1_safe_receiver == null ? null : tmp1_safe_receiver(serializedClassName);
  };
  protoOf(SerialModuleImpl).mg = function (kClass, typeArgumentsSerializers) {
    var tmp0_safe_receiver = this.ry_1.b2(kClass);
    var tmp = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.xy(typeArgumentsSerializers);
    return (tmp == null ? true : isInterface(tmp, KSerializer)) ? tmp : null;
  };
  protoOf(SerialModuleImpl).qy = function (collector) {
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s = this.ry_1.e2().r();
    while (_iterator__ex2g4s.s()) {
      var element = _iterator__ex2g4s.t();
      // Inline function 'kotlin.collections.component1' call
      var kclass = element.x1();
      // Inline function 'kotlin.collections.component2' call
      var serial = element.y1();
      if (serial instanceof Argless) {
        var tmp = isInterface(kclass, KClass) ? kclass : THROW_CCE();
        var tmp_0 = serial.az_1;
        collector.bz(tmp, isInterface(tmp_0, KSerializer) ? tmp_0 : THROW_CCE());
      } else {
        if (serial instanceof WithTypeArguments) {
          collector.zy(kclass, serial.yy_1);
        } else {
          noWhenBranchMatchedException();
        }
      }
    }
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s_0 = this.sy_1.e2().r();
    while (_iterator__ex2g4s_0.s()) {
      var element_0 = _iterator__ex2g4s_0.t();
      // Inline function 'kotlin.collections.component1' call
      var baseClass = element_0.x1();
      // Inline function 'kotlin.collections.component2' call
      var classMap = element_0.y1();
      // Inline function 'kotlin.collections.forEach' call
      // Inline function 'kotlin.collections.iterator' call
      var _iterator__ex2g4s_1 = classMap.e2().r();
      while (_iterator__ex2g4s_1.s()) {
        var element_1 = _iterator__ex2g4s_1.t();
        // Inline function 'kotlin.collections.component1' call
        var actualClass = element_1.x1();
        // Inline function 'kotlin.collections.component2' call
        var serializer = element_1.y1();
        var tmp_1 = isInterface(baseClass, KClass) ? baseClass : THROW_CCE();
        var tmp_2 = isInterface(actualClass, KClass) ? actualClass : THROW_CCE();
        // Inline function 'kotlinx.serialization.internal.cast' call
        var tmp$ret$15 = isInterface(serializer, KSerializer) ? serializer : THROW_CCE();
        collector.cz(tmp_1, tmp_2, tmp$ret$15);
      }
    }
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s_2 = this.ty_1.e2().r();
    while (_iterator__ex2g4s_2.s()) {
      var element_2 = _iterator__ex2g4s_2.t();
      // Inline function 'kotlin.collections.component1' call
      var baseClass_0 = element_2.x1();
      // Inline function 'kotlin.collections.component2' call
      var provider = element_2.y1();
      var tmp_3 = isInterface(baseClass_0, KClass) ? baseClass_0 : THROW_CCE();
      collector.dz(tmp_3, typeof provider === 'function' ? provider : THROW_CCE());
    }
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s_3 = this.vy_1.e2().r();
    while (_iterator__ex2g4s_3.s()) {
      var element_3 = _iterator__ex2g4s_3.t();
      // Inline function 'kotlin.collections.component1' call
      var baseClass_1 = element_3.x1();
      // Inline function 'kotlin.collections.component2' call
      var provider_0 = element_3.y1();
      var tmp_4 = isInterface(baseClass_1, KClass) ? baseClass_1 : THROW_CCE();
      collector.ez(tmp_4, typeof provider_0 === 'function' ? provider_0 : THROW_CCE());
    }
  };
  function Argless() {
  }
  function WithTypeArguments() {
  }
  function ContextualProvider() {
  }
  var properties_initialized_SerializersModule_kt_fjigjn;
  function _init_properties_SerializersModule_kt__u78ha3() {
    if (!properties_initialized_SerializersModule_kt_fjigjn) {
      properties_initialized_SerializersModule_kt_fjigjn = true;
      EmptySerializersModule = new SerialModuleImpl(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), false);
    }
  }
  function EmptySerializersModule_0() {
    return get_EmptySerializersModuleLegacyJs();
  }
  function SerializersModuleCollector$contextual$lambda($serializer) {
    return function (it) {
      return $serializer;
    };
  }
  function SerializersModuleCollector() {
  }
  function SerializableWith(serializer) {
    this.fz_1 = serializer;
  }
  protoOf(SerializableWith).equals = function (other) {
    if (!(other instanceof SerializableWith))
      return false;
    var tmp0_other_with_cast = other instanceof SerializableWith ? other : THROW_CCE();
    if (!this.fz_1.equals(tmp0_other_with_cast.fz_1))
      return false;
    return true;
  };
  protoOf(SerializableWith).hashCode = function () {
    return imul(getStringHashCode('serializer'), 127) ^ this.fz_1.hashCode();
  };
  protoOf(SerializableWith).toString = function () {
    return '@kotlinx.serialization.SerializableWith(' + 'serializer=' + toString(this.fz_1) + ')';
  };
  function createCache(factory) {
    return new createCache$1(factory);
  }
  function isInterface_0(_this__u8e3s4) {
    return get_isInterfaceHack(_this__u8e3s4);
  }
  function createParametrizedCache(factory) {
    return new createParametrizedCache$1(factory);
  }
  function getChecked(_this__u8e3s4, index) {
    if (!(0 <= index ? index <= (_this__u8e3s4.length - 1 | 0) : false))
      throw IndexOutOfBoundsException_init_$Create$('Index ' + index + ' out of bounds ' + get_indices(_this__u8e3s4).toString());
    return _this__u8e3s4[index];
  }
  function getChecked_0(_this__u8e3s4, index) {
    if (!(0 <= index ? index <= (_this__u8e3s4.length - 1 | 0) : false))
      throw IndexOutOfBoundsException_init_$Create$('Index ' + index + ' out of bounds ' + get_indices_0(_this__u8e3s4).toString());
    return _this__u8e3s4[index];
  }
  function initBuiltins() {
    return mapOf([to(PrimitiveClasses_getInstance().y8(), serializer_0(StringCompanionObject_instance)), to(getKClass(Char), serializer_9(Companion_getInstance_2())), to(PrimitiveClasses_getInstance().b9(), CharArraySerializer()), to(PrimitiveClasses_getInstance().w8(), serializer_10(DoubleCompanionObject_instance)), to(PrimitiveClasses_getInstance().i9(), DoubleArraySerializer()), to(PrimitiveClasses_getInstance().v8(), serializer_11(FloatCompanionObject_instance)), to(PrimitiveClasses_getInstance().h9(), FloatArraySerializer()), to(PrimitiveClasses_getInstance().u8(), serializer_6(Companion_getInstance_3())), to(PrimitiveClasses_getInstance().g9(), LongArraySerializer()), to(getKClass(ULong), serializer_2(Companion_getInstance_4())), to(getKClass(ULongArray), ULongArraySerializer()), to(PrimitiveClasses_getInstance().t8(), serializer_5(IntCompanionObject_instance)), to(PrimitiveClasses_getInstance().e9(), IntArraySerializer()), to(getKClass(UInt), serializer_1(Companion_getInstance_5())), to(getKClass(UIntArray), UIntArraySerializer()), to(PrimitiveClasses_getInstance().s8(), serializer_8(ShortCompanionObject_instance)), to(PrimitiveClasses_getInstance().d9(), ShortArraySerializer()), to(getKClass(UShort), serializer_4(Companion_getInstance_6())), to(getKClass(UShortArray), UShortArraySerializer()), to(PrimitiveClasses_getInstance().r8(), serializer_7(ByteCompanionObject_instance)), to(PrimitiveClasses_getInstance().c9(), ByteArraySerializer()), to(getKClass(UByte), serializer_3(Companion_getInstance_7())), to(getKClass(UByteArray), UByteArraySerializer()), to(PrimitiveClasses_getInstance().q8(), serializer_12(BooleanCompanionObject_instance)), to(PrimitiveClasses_getInstance().a9(), BooleanArraySerializer()), to(getKClass(Unit), serializer_13(Unit_instance)), to(PrimitiveClasses_getInstance().p8(), NothingSerializer()), to(getKClass(Duration), serializer_14(Companion_getInstance())), to(getKClass(Instant), serializer_15(Companion_getInstance_0())), to(getKClass(Uuid), serializer_16(Companion_getInstance_1()))]);
  }
  function get_isInterfaceHack(_this__u8e3s4) {
    if (_this__u8e3s4 === PrimitiveClasses_getInstance().p8())
      return false;
    // Inline function 'kotlin.js.asDynamic' call
    var tmp0_safe_receiver = get_js(_this__u8e3s4).$metadata$;
    return (tmp0_safe_receiver == null ? null : tmp0_safe_receiver.kind) == 'interface';
  }
  function compiledSerializerImpl(_this__u8e3s4) {
    var tmp0_elvis_lhs = constructSerializerForGivenTypeArgs(_this__u8e3s4, []);
    var tmp;
    if (tmp0_elvis_lhs == null) {
      var tmp_0;
      if (_this__u8e3s4 === PrimitiveClasses_getInstance().p8()) {
        tmp_0 = NothingSerializer_getInstance();
      } else {
        // Inline function 'kotlin.js.asDynamic' call
        var tmp1_safe_receiver = get_js(_this__u8e3s4).Companion;
        tmp_0 = tmp1_safe_receiver == null ? null : tmp1_safe_receiver.serializer();
      }
      var tmp_1 = tmp_0;
      tmp = (!(tmp_1 == null) ? isInterface(tmp_1, KSerializer) : false) ? tmp_1 : null;
    } else {
      tmp = tmp0_elvis_lhs;
    }
    return tmp;
  }
  function platformSpecificSerializerNotRegistered(_this__u8e3s4) {
    throw SerializationException_init_$Create$_0(notRegisteredMessage(_this__u8e3s4) + 'To get enum serializer on Kotlin/JS, it should be annotated with @Serializable annotation.');
  }
  function isReferenceArray(rootClass) {
    return rootClass.equals(PrimitiveClasses_getInstance().x8());
  }
  function constructSerializerForGivenTypeArgs(_this__u8e3s4, args) {
    var tmp;
    try {
      // Inline function 'kotlin.reflect.findAssociatedObject' call
      var assocObject = findAssociatedObject(_this__u8e3s4, getKClass(SerializableWith));
      var tmp_0;
      if (!(assocObject == null) ? isInterface(assocObject, KSerializer) : false) {
        tmp_0 = isInterface(assocObject, KSerializer) ? assocObject : THROW_CCE();
      } else {
        if (!(assocObject == null) ? isInterface(assocObject, SerializerFactory) : false) {
          var tmp_1 = assocObject.br(args.slice());
          tmp_0 = isInterface(tmp_1, KSerializer) ? tmp_1 : THROW_CCE();
        } else {
          tmp_0 = null;
        }
      }
      tmp = tmp_0;
    } catch ($p) {
      var tmp_2;
      var e = $p;
      tmp_2 = null;
      tmp = tmp_2;
    }
    return tmp;
  }
  function toNativeArrayImpl(_this__u8e3s4, eClass) {
    // Inline function 'kotlin.collections.toTypedArray' call
    return copyToArray(_this__u8e3s4);
  }
  function estimateCapacityForHashMap(requiredCapacity) {
    return requiredCapacity;
  }
  function createCache$1($factory) {
    this.gz_1 = $factory;
  }
  protoOf(createCache$1).ng = function (key) {
    return this.gz_1(key);
  };
  function createParametrizedCache$1($factory) {
    this.hz_1 = $factory;
  }
  protoOf(createParametrizedCache$1).og = function (key, types) {
    // Inline function 'kotlin.runCatching' call
    var tmp;
    try {
      // Inline function 'kotlin.Companion.success' call
      var value = this.hz_1(key, types);
      tmp = _Result___init__impl__xyqfz8(value);
    } catch ($p) {
      var tmp_0;
      if ($p instanceof Error) {
        var e = $p;
        // Inline function 'kotlin.Companion.failure' call
        tmp_0 = _Result___init__impl__xyqfz8(createFailure(e));
      } else {
        throw $p;
      }
      tmp = tmp_0;
    }
    return tmp;
  };
  //region block: post-declaration
  protoOf(SerialDescriptorImpl).pg = get_isNullable;
  protoOf(SerialDescriptorImpl).vg = get_isInline;
  protoOf(AbstractDecoder).dj = decodeSerializableElement$default;
  protoOf(AbstractDecoder).pi = decodeSerializableValue;
  protoOf(AbstractDecoder).gj = decodeSequentially;
  protoOf(AbstractDecoder).ij = decodeCollectionSize;
  protoOf(AbstractEncoder).lk = encodeNotNullMark;
  protoOf(AbstractEncoder).mk = beginCollection;
  protoOf(AbstractEncoder).ik = encodeSerializableValue;
  protoOf(AbstractEncoder).kk = encodeNullableSerializableValue;
  protoOf(AbstractEncoder).nk = shouldEncodeElementDefault;
  protoOf(ListLikeDescriptor).pg = get_isNullable;
  protoOf(ListLikeDescriptor).vg = get_isInline;
  protoOf(ListLikeDescriptor).xg = get_annotations;
  protoOf(MapLikeDescriptor).pg = get_isNullable;
  protoOf(MapLikeDescriptor).vg = get_isInline;
  protoOf(MapLikeDescriptor).xg = get_annotations;
  protoOf(PluginGeneratedSerialDescriptor).pg = get_isNullable;
  protoOf(PluginGeneratedSerialDescriptor).vg = get_isInline;
  protoOf(InlinePrimitiveDescriptor$1).pq = typeParametersSerializers;
  protoOf(NothingSerialDescriptor).pg = get_isNullable;
  protoOf(NothingSerialDescriptor).vg = get_isInline;
  protoOf(NothingSerialDescriptor).xg = get_annotations;
  protoOf(PrimitiveSerialDescriptor_0).pg = get_isNullable;
  protoOf(PrimitiveSerialDescriptor_0).vg = get_isInline;
  protoOf(PrimitiveSerialDescriptor_0).xg = get_annotations;
  protoOf(TaggedDecoder).dj = decodeSerializableElement$default;
  protoOf(TaggedDecoder).pi = decodeSerializableValue;
  protoOf(TaggedDecoder).gj = decodeSequentially;
  protoOf(TaggedDecoder).ij = decodeCollectionSize;
  //endregion
  //region block: exports
  _.$_$ = _.$_$ || {};
  _.$_$.a = SEALED_getInstance;
  _.$_$.b = STRING_getInstance;
  _.$_$.c = CONTEXTUAL_getInstance;
  _.$_$.d = ENUM_getInstance;
  _.$_$.e = CLASS_getInstance;
  _.$_$.f = LIST_getInstance;
  _.$_$.g = MAP_getInstance;
  _.$_$.h = OBJECT_getInstance;
  _.$_$.i = BooleanSerializer_getInstance;
  _.$_$.j = IntSerializer_getInstance;
  _.$_$.k = LongSerializer_getInstance;
  _.$_$.l = StringSerializer_getInstance;
  _.$_$.m = SerializationException_init_$Create$_0;
  _.$_$.n = SerializationException_init_$Init$_1;
  _.$_$.o = UnknownFieldException_init_$Create$;
  _.$_$.p = ListSerializer;
  _.$_$.q = MapSerializer;
  _.$_$.r = get_nullable;
  _.$_$.s = serializer_0;
  _.$_$.t = serializer_4;
  _.$_$.u = serializer_1;
  _.$_$.v = serializer_3;
  _.$_$.w = serializer_2;
  _.$_$.x = PolymorphicKind;
  _.$_$.y = PrimitiveKind;
  _.$_$.z = PrimitiveSerialDescriptor;
  _.$_$.a1 = get_annotations;
  _.$_$.b1 = get_isInline;
  _.$_$.c1 = get_isNullable;
  _.$_$.d1 = SerialDescriptor;
  _.$_$.e1 = ENUM;
  _.$_$.f1 = buildSerialDescriptor;
  _.$_$.g1 = getContextualDescriptor;
  _.$_$.h1 = AbstractDecoder;
  _.$_$.i1 = AbstractEncoder;
  _.$_$.j1 = CompositeDecoder;
  _.$_$.k1 = CompositeEncoder;
  _.$_$.l1 = Decoder;
  _.$_$.m1 = Encoder;
  _.$_$.n1 = AbstractPolymorphicSerializer;
  _.$_$.o1 = ArrayListSerializer;
  _.$_$.p1 = ElementMarker;
  _.$_$.q1 = typeParametersSerializers;
  _.$_$.r1 = GeneratedSerializer;
  _.$_$.s1 = InlinePrimitiveDescriptor;
  _.$_$.t1 = NamedValueDecoder;
  _.$_$.u1 = PluginGeneratedSerialDescriptor;
  _.$_$.v1 = SerializerFactory;
  _.$_$.w1 = createSimpleEnumSerializer;
  _.$_$.x1 = jsonCachedSerialNames;
  _.$_$.y1 = missingFieldExceptionWithNewMessage;
  _.$_$.z1 = throwMissingFieldException;
  _.$_$.a2 = EmptySerializersModule_0;
  _.$_$.b2 = contextual;
  _.$_$.c2 = SerializersModuleCollector;
  _.$_$.d2 = DeserializationStrategy;
  _.$_$.e2 = KSerializer;
  _.$_$.f2 = MissingFieldException;
  _.$_$.g2 = SerializableWith;
  _.$_$.h2 = SerializationException;
  _.$_$.i2 = SerializationStrategy;
  _.$_$.j2 = serializer;
  //endregion
  return _;
}));

//# sourceMappingURL=kotlinx-serialization-kotlinx-serialization-core.js.map
