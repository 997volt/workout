(function (factory) {
  if (typeof define === 'function' && define.amd)
    define(['exports', './kotlin-kotlin-stdlib.js', './kotlinx-serialization-kotlinx-serialization-json.js', './kotlinx-serialization-kotlinx-serialization-core.js'], factory);
  else if (typeof exports === 'object')
    factory(module.exports, require('./kotlin-kotlin-stdlib.js'), require('./kotlinx-serialization-kotlinx-serialization-json.js'), require('./kotlinx-serialization-kotlinx-serialization-core.js'));
  else {
    if (typeof globalThis['kotlin-kotlin-stdlib'] === 'undefined') {
      throw new Error("Error loading module 'workout:shared'. Its dependency 'kotlin-kotlin-stdlib' was not found. Please, check whether 'kotlin-kotlin-stdlib' is loaded prior to 'workout:shared'.");
    }
    if (typeof globalThis['kotlinx-serialization-kotlinx-serialization-json'] === 'undefined') {
      throw new Error("Error loading module 'workout:shared'. Its dependency 'kotlinx-serialization-kotlinx-serialization-json' was not found. Please, check whether 'kotlinx-serialization-kotlinx-serialization-json' is loaded prior to 'workout:shared'.");
    }
    if (typeof globalThis['kotlinx-serialization-kotlinx-serialization-core'] === 'undefined') {
      throw new Error("Error loading module 'workout:shared'. Its dependency 'kotlinx-serialization-kotlinx-serialization-core' was not found. Please, check whether 'kotlinx-serialization-kotlinx-serialization-core' is loaded prior to 'workout:shared'.");
    }
    globalThis['workout:shared'] = factory(typeof globalThis['workout:shared'] === 'undefined' ? {} : globalThis['workout:shared'], globalThis['kotlin-kotlin-stdlib'], globalThis['kotlinx-serialization-kotlinx-serialization-json'], globalThis['kotlinx-serialization-kotlinx-serialization-core']);
  }
}(function (_, kotlin_kotlin, kotlin_org_jetbrains_kotlinx_kotlinx_serialization_json, kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core) {
  'use strict';
  //region block: imports
  var imul = Math.imul;
  var Unit_instance = kotlin_kotlin.$_$.o2;
  var Enum = kotlin_kotlin.$_$.r9;
  var protoOf = kotlin_kotlin.$_$.a8;
  var initMetadataForClass = kotlin_kotlin.$_$.g7;
  var VOID = kotlin_kotlin.$_$.b;
  var getStringHashCode = kotlin_kotlin.$_$.e7;
  var Exception = kotlin_kotlin.$_$.s9;
  var Exception_init_$Init$ = kotlin_kotlin.$_$.g3;
  var captureStack = kotlin_kotlin.$_$.t6;
  var Default_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_json.$_$.a;
  var SerializationException = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.h2;
  var JsonObject = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_json.$_$.b;
  var JsonPrimitive = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_json.$_$.c;
  var get_intOrNull = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_json.$_$.e;
  var getKClass = kotlin_kotlin.$_$.l8;
  var arrayOf = kotlin_kotlin.$_$.ia;
  var createKType = kotlin_kotlin.$_$.j8;
  var serializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.j2;
  var THROW_CCE = kotlin_kotlin.$_$.x9;
  var KSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.e2;
  var isInterface = kotlin_kotlin.$_$.t7;
  var Json = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_json.$_$.d;
  var initMetadataForObject = kotlin_kotlin.$_$.l7;
  var collectionSizeOrDefault = kotlin_kotlin.$_$.e4;
  var ArrayList_init_$Create$ = kotlin_kotlin.$_$.p2;
  var listOf = kotlin_kotlin.$_$.g5;
  var noWhenBranchMatchedException = kotlin_kotlin.$_$.ra;
  var initMetadataForCompanion = kotlin_kotlin.$_$.h7;
  var PluginGeneratedSerialDescriptor = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.u1;
  var StringSerializer_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.l;
  var UnknownFieldException_init_$Create$ = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.o;
  var get_nullable = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.r;
  var typeParametersSerializers = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.q1;
  var GeneratedSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.r1;
  var throwMissingFieldException = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.z1;
  var objectCreate = kotlin_kotlin.$_$.z7;
  var ArrayListSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.o1;
  var LazyThreadSafetyMode_PUBLICATION_getInstance = kotlin_kotlin.$_$.u1;
  var lazy = kotlin_kotlin.$_$.pa;
  var toString = kotlin_kotlin.$_$.c8;
  var hashCode = kotlin_kotlin.$_$.f7;
  var equals = kotlin_kotlin.$_$.b7;
  var emptyList = kotlin_kotlin.$_$.u4;
  var BooleanSerializer_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.i;
  var getBooleanHashCode = kotlin_kotlin.$_$.c7;
  var SerializableWith = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.g2;
  var Long = kotlin_kotlin.$_$.u9;
  var IntSerializer_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.j;
  var LongSerializer_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.k;
  var toString_0 = kotlin_kotlin.$_$.ta;
  var equalsLong = kotlin_kotlin.$_$.g6;
  var createSimpleEnumSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.w1;
  var Companion_instance = kotlin_kotlin.$_$.h2;
  var SerializerFactory = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.v1;
  var ArrayList_init_$Create$_0 = kotlin_kotlin.$_$.q2;
  var compare = kotlin_kotlin.$_$.c6;
  var mapCapacity = kotlin_kotlin.$_$.h5;
  var coerceAtLeast = kotlin_kotlin.$_$.e8;
  var LinkedHashMap_init_$Create$ = kotlin_kotlin.$_$.y2;
  var toSet = kotlin_kotlin.$_$.t5;
  var addAll = kotlin_kotlin.$_$.b4;
  var distinct = kotlin_kotlin.$_$.s4;
  var plus = kotlin_kotlin.$_$.j5;
  var isBlank = kotlin_kotlin.$_$.t8;
  var checkIndexOverflow = kotlin_kotlin.$_$.d4;
  var LinkedHashMap_init_$Create$_0 = kotlin_kotlin.$_$.z2;
  var sortedWith = kotlin_kotlin.$_$.n5;
  var getOrNull = kotlin_kotlin.$_$.w4;
  var eachCount = kotlin_kotlin.$_$.t4;
  var FunctionAdapter = kotlin_kotlin.$_$.q6;
  var Comparator = kotlin_kotlin.$_$.o9;
  var compareValues = kotlin_kotlin.$_$.w5;
  //endregion
  //region block: pre-declaration
  initMetadataForClass(ProblemCode, 'ProblemCode', VOID, Enum);
  initMetadataForClass(ProgramProblem, 'ProgramProblem');
  initMetadataForClass(ProgramFormatException, 'ProgramFormatException', VOID, Exception);
  initMetadataForObject(ProgramDocumentCodec, 'ProgramDocumentCodec');
  initMetadataForCompanion(Companion);
  initMetadataForObject($serializer, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(ProblemOut, 'ProblemOut', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance});
  initMetadataForCompanion(Companion_0);
  initMetadataForObject($serializer_0, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(ChangeOut, 'ChangeOut', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_0});
  initMetadataForCompanion(Companion_1);
  initMetadataForObject($serializer_1, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(WarningOut, 'WarningOut', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_1});
  initMetadataForCompanion(Companion_2);
  initMetadataForObject($serializer_2, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(ValidateOut, 'ValidateOut', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_2});
  initMetadataForCompanion(Companion_3);
  initMetadataForObject($serializer_3, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(PlanOut, 'PlanOut', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_3});
  initMetadataForCompanion(Companion_4);
  initMetadataForObject($serializer_4, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(ProgramDocument, 'ProgramDocument', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_4});
  initMetadataForObject($serializer_5, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(EditMarker, 'EditMarker', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_5});
  initMetadataForCompanion(Companion_5);
  initMetadataForObject($serializer_6, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(ProgramDto, 'ProgramDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_6});
  initMetadataForCompanion(Companion_6, VOID, [SerializerFactory]);
  initMetadataForClass(ProgramKind, 'ProgramKind', VOID, Enum, VOID, VOID, VOID, {0: Companion_getInstance_6});
  initMetadataForCompanion(Companion_7);
  initMetadataForObject($serializer_7, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(ProgramSlotDto, 'ProgramSlotDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_7});
  initMetadataForObject($serializer_8, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(TemplateDto, 'TemplateDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_8});
  initMetadataForObject($serializer_9, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(TemplateExerciseDto, 'TemplateExerciseDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_9});
  initMetadataForCompanion(Companion_8);
  initMetadataForObject($serializer_10, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(TemplateSetDto, 'TemplateSetDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_10});
  initMetadataForCompanion(Companion_9);
  initMetadataForObject($serializer_11, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(ExerciseDto, 'ExerciseDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_11});
  initMetadataForCompanion(Companion_10, VOID, [SerializerFactory]);
  initMetadataForClass(Weekday, 'Weekday', VOID, Enum, VOID, VOID, VOID, {0: Companion_getInstance_10});
  initMetadataForCompanion(Companion_11, VOID, [SerializerFactory]);
  initMetadataForClass(ProgramSetRole, 'ProgramSetRole', VOID, Enum, VOID, VOID, VOID, {0: Companion_getInstance_11});
  initMetadataForClass(ChangeKind, 'ChangeKind', VOID, Enum);
  initMetadataForClass(Change, 'Change');
  initMetadataForClass(WarningCode, 'WarningCode', VOID, Enum);
  initMetadataForClass(PlanWarning, 'PlanWarning');
  initMetadataForClass(Ready, 'Ready');
  initMetadataForClass(Refused, 'Refused');
  initMetadataForClass(Refusal, 'Refusal');
  initMetadataForClass(sam$kotlin_Comparator$0, 'sam$kotlin_Comparator$0', VOID, VOID, [Comparator, FunctionAdapter]);
  initMetadataForClass(duplicates$$inlined$groupingBy$1);
  //endregion
  var static_init_called;
  function static_init() {
    if (static_init_called)
      return Unit_instance;
    static_init_called = true;
    ProblemCode_MALFORMED_instance = new ProblemCode('MALFORMED', 0);
    ProblemCode_MISSING_VERSION_instance = new ProblemCode('MISSING_VERSION', 1);
    ProblemCode_NEWER_VERSION_instance = new ProblemCode('NEWER_VERSION', 2);
    ProblemCode_OLDER_VERSION_instance = new ProblemCode('OLDER_VERSION', 3);
    ProblemCode_NO_EDIT_MARKER_instance = new ProblemCode('NO_EDIT_MARKER', 4);
    ProblemCode_STALE_BASE_instance = new ProblemCode('STALE_BASE', 5);
    ProblemCode_PROGRAM_MISMATCH_instance = new ProblemCode('PROGRAM_MISMATCH', 6);
    ProblemCode_PROGRAM_MISSING_instance = new ProblemCode('PROGRAM_MISSING', 7);
    ProblemCode_DUPLICATE_ID_instance = new ProblemCode('DUPLICATE_ID', 8);
    ProblemCode_DANGLING_SLOT_TEMPLATE_instance = new ProblemCode('DANGLING_SLOT_TEMPLATE', 9);
    ProblemCode_DANGLING_TEMPLATE_EXERCISE_instance = new ProblemCode('DANGLING_TEMPLATE_EXERCISE', 10);
    ProblemCode_DANGLING_SET_instance = new ProblemCode('DANGLING_SET', 11);
    ProblemCode_DANGLING_EXERCISE_instance = new ProblemCode('DANGLING_EXERCISE', 12);
    ProblemCode_EMPTY_NAME_instance = new ProblemCode('EMPTY_NAME', 13);
    ProblemCode_INVALID_POSITION_instance = new ProblemCode('INVALID_POSITION', 14);
    ProblemCode_INVALID_SET_INDEX_instance = new ProblemCode('INVALID_SET_INDEX', 15);
    ProblemCode_INVALID_REPS_instance = new ProblemCode('INVALID_REPS', 16);
    ProblemCode_INVALID_LOAD_instance = new ProblemCode('INVALID_LOAD', 17);
    ProblemCode_INVALID_RPE_instance = new ProblemCode('INVALID_RPE', 18);
    ProblemCode_INVALID_REST_instance = new ProblemCode('INVALID_REST', 19);
    ProblemCode_RUNG_WITHOUT_ANCHOR_instance = new ProblemCode('RUNG_WITHOUT_ANCHOR', 20);
    ProblemCode_RUNG_UNDER_WARMUP_instance = new ProblemCode('RUNG_UNDER_WARMUP', 21);
    ProblemCode_RUNG_WITHOUT_VALUE_instance = new ProblemCode('RUNG_WITHOUT_VALUE', 22);
    ProblemCode_RUN_MIXES_KINDS_instance = new ProblemCode('RUN_MIXES_KINDS', 23);
    ProblemCode_RUNG_CARRIES_TARGETS_instance = new ProblemCode('RUNG_CARRIES_TARGETS', 24);
  }
  var ProblemCode_MALFORMED_instance;
  var ProblemCode_MISSING_VERSION_instance;
  var ProblemCode_NEWER_VERSION_instance;
  var ProblemCode_OLDER_VERSION_instance;
  var ProblemCode_NO_EDIT_MARKER_instance;
  var ProblemCode_STALE_BASE_instance;
  var ProblemCode_PROGRAM_MISMATCH_instance;
  var ProblemCode_PROGRAM_MISSING_instance;
  var ProblemCode_DUPLICATE_ID_instance;
  var ProblemCode_DANGLING_SLOT_TEMPLATE_instance;
  var ProblemCode_DANGLING_TEMPLATE_EXERCISE_instance;
  var ProblemCode_DANGLING_SET_instance;
  var ProblemCode_DANGLING_EXERCISE_instance;
  var ProblemCode_EMPTY_NAME_instance;
  var ProblemCode_INVALID_POSITION_instance;
  var ProblemCode_INVALID_SET_INDEX_instance;
  var ProblemCode_INVALID_REPS_instance;
  var ProblemCode_INVALID_LOAD_instance;
  var ProblemCode_INVALID_RPE_instance;
  var ProblemCode_INVALID_REST_instance;
  var ProblemCode_RUNG_WITHOUT_ANCHOR_instance;
  var ProblemCode_RUNG_UNDER_WARMUP_instance;
  var ProblemCode_RUNG_WITHOUT_VALUE_instance;
  var ProblemCode_RUN_MIXES_KINDS_instance;
  var ProblemCode_RUNG_CARRIES_TARGETS_instance;
  function ProblemCode(name, ordinal) {
    Enum.call(this, name, ordinal);
  }
  function ProgramProblem(code, path, detail) {
    path = path === VOID ? '' : path;
    detail = detail === VOID ? null : detail;
    this.o1b_1 = code;
    this.p1b_1 = path;
    this.q1b_1 = detail;
  }
  protoOf(ProgramProblem).toString = function () {
    return 'ProgramProblem(code=' + this.o1b_1.toString() + ', path=' + this.p1b_1 + ', detail=' + this.q1b_1 + ')';
  };
  protoOf(ProgramProblem).hashCode = function () {
    var result = this.o1b_1.hashCode();
    result = imul(result, 31) + getStringHashCode(this.p1b_1) | 0;
    result = imul(result, 31) + (this.q1b_1 == null ? 0 : getStringHashCode(this.q1b_1)) | 0;
    return result;
  };
  protoOf(ProgramProblem).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ProgramProblem))
      return false;
    if (!this.o1b_1.equals(other.o1b_1))
      return false;
    if (!(this.p1b_1 === other.p1b_1))
      return false;
    if (!(this.q1b_1 == other.q1b_1))
      return false;
    return true;
  };
  function ProgramFormatException(problem, cause) {
    cause = cause === VOID ? null : cause;
    Exception_init_$Init$(problem.o1b_1.j2_1, cause, this);
    captureStack(this, ProgramFormatException);
    this.r1b_1 = problem;
  }
  function parseOrRefuse($this, text) {
    var tmp;
    try {
      tmp = Default_getInstance().oz(text);
    } catch ($p) {
      var tmp_0;
      if ($p instanceof SerializationException) {
        var malformed = $p;
        throw new ProgramFormatException(new ProgramProblem(ProblemCode_MALFORMED_getInstance()), malformed);
      } else {
        throw $p;
      }
    }
    return tmp;
  }
  function versionOf($this, element) {
    var tmp0_safe_receiver = element instanceof JsonObject ? element : null;
    var tmp = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.y11('formatVersion');
    var tmp1_safe_receiver = tmp instanceof JsonPrimitive ? tmp : null;
    var tmp2_elvis_lhs = tmp1_safe_receiver == null ? null : get_intOrNull(tmp1_safe_receiver);
    var tmp_0;
    if (tmp2_elvis_lhs == null) {
      throw new ProgramFormatException(new ProgramProblem(ProblemCode_MISSING_VERSION_getInstance()));
    } else {
      tmp_0 = tmp2_elvis_lhs;
    }
    return tmp_0;
  }
  function refuseUnreadable($this, version) {
    var tmp;
    if (version > 6) {
      tmp = new ProgramProblem(ProblemCode_NEWER_VERSION_getInstance(), VOID, version.toString());
    } else if (version < 4) {
      tmp = new ProgramProblem(ProblemCode_OLDER_VERSION_getInstance(), VOID, version.toString());
    } else {
      return Unit_instance;
    }
    var problem = tmp;
    throw new ProgramFormatException(problem);
  }
  function bodyOrRefuse($this, element) {
    var tmp;
    try {
      // Inline function 'kotlinx.serialization.json.decodeFromJsonElement' call
      var this_0 = $this.u1b_1;
      // Inline function 'kotlinx.serialization.serializer' call
      var this_1 = this_0.fj();
      // Inline function 'kotlinx.serialization.internal.cast' call
      var this_2 = serializer(this_1, createKType(getKClass(ProgramDocument), arrayOf([]), false));
      var tmp$ret$1 = isInterface(this_2, KSerializer) ? this_2 : THROW_CCE();
      tmp = this_0.nz(tmp$ret$1, element);
    } catch ($p) {
      var tmp_0;
      if ($p instanceof SerializationException) {
        var malformed = $p;
        throw new ProgramFormatException(new ProgramProblem(ProblemCode_MALFORMED_getInstance()), malformed);
      } else {
        throw $p;
      }
    }
    return tmp;
  }
  function ProgramDocumentCodec$json$lambda($this$Json) {
    $this$Json.j10_1 = true;
    $this$Json.h10_1 = true;
    $this$Json.f10_1 = true;
    return Unit_instance;
  }
  function ProgramDocumentCodec() {
    ProgramDocumentCodec_instance = this;
    this.s1b_1 = 6;
    this.t1b_1 = 4;
    var tmp = this;
    tmp.u1b_1 = Json(VOID, ProgramDocumentCodec$json$lambda);
  }
  protoOf(ProgramDocumentCodec).v1b = function (text) {
    var element = parseOrRefuse(this, text);
    refuseUnreadable(this, versionOf(this, element));
    return bodyOrRefuse(this, element);
  };
  var ProgramDocumentCodec_instance;
  function ProgramDocumentCodec_getInstance() {
    if (ProgramDocumentCodec_instance == null)
      new ProgramDocumentCodec();
    return ProgramDocumentCodec_instance;
  }
  function ProblemCode_MALFORMED_getInstance() {
    static_init();
    return ProblemCode_MALFORMED_instance;
  }
  function ProblemCode_MISSING_VERSION_getInstance() {
    static_init();
    return ProblemCode_MISSING_VERSION_instance;
  }
  function ProblemCode_NEWER_VERSION_getInstance() {
    static_init();
    return ProblemCode_NEWER_VERSION_instance;
  }
  function ProblemCode_OLDER_VERSION_getInstance() {
    static_init();
    return ProblemCode_OLDER_VERSION_instance;
  }
  function ProblemCode_NO_EDIT_MARKER_getInstance() {
    static_init();
    return ProblemCode_NO_EDIT_MARKER_instance;
  }
  function ProblemCode_STALE_BASE_getInstance() {
    static_init();
    return ProblemCode_STALE_BASE_instance;
  }
  function ProblemCode_PROGRAM_MISMATCH_getInstance() {
    static_init();
    return ProblemCode_PROGRAM_MISMATCH_instance;
  }
  function ProblemCode_PROGRAM_MISSING_getInstance() {
    static_init();
    return ProblemCode_PROGRAM_MISSING_instance;
  }
  function ProblemCode_DUPLICATE_ID_getInstance() {
    static_init();
    return ProblemCode_DUPLICATE_ID_instance;
  }
  function ProblemCode_DANGLING_SLOT_TEMPLATE_getInstance() {
    static_init();
    return ProblemCode_DANGLING_SLOT_TEMPLATE_instance;
  }
  function ProblemCode_DANGLING_TEMPLATE_EXERCISE_getInstance() {
    static_init();
    return ProblemCode_DANGLING_TEMPLATE_EXERCISE_instance;
  }
  function ProblemCode_DANGLING_SET_getInstance() {
    static_init();
    return ProblemCode_DANGLING_SET_instance;
  }
  function ProblemCode_DANGLING_EXERCISE_getInstance() {
    static_init();
    return ProblemCode_DANGLING_EXERCISE_instance;
  }
  function ProblemCode_EMPTY_NAME_getInstance() {
    static_init();
    return ProblemCode_EMPTY_NAME_instance;
  }
  function ProblemCode_INVALID_POSITION_getInstance() {
    static_init();
    return ProblemCode_INVALID_POSITION_instance;
  }
  function ProblemCode_INVALID_SET_INDEX_getInstance() {
    static_init();
    return ProblemCode_INVALID_SET_INDEX_instance;
  }
  function ProblemCode_INVALID_REPS_getInstance() {
    static_init();
    return ProblemCode_INVALID_REPS_instance;
  }
  function ProblemCode_INVALID_LOAD_getInstance() {
    static_init();
    return ProblemCode_INVALID_LOAD_instance;
  }
  function ProblemCode_INVALID_RPE_getInstance() {
    static_init();
    return ProblemCode_INVALID_RPE_instance;
  }
  function ProblemCode_INVALID_REST_getInstance() {
    static_init();
    return ProblemCode_INVALID_REST_instance;
  }
  function ProblemCode_RUNG_WITHOUT_ANCHOR_getInstance() {
    static_init();
    return ProblemCode_RUNG_WITHOUT_ANCHOR_instance;
  }
  function ProblemCode_RUNG_UNDER_WARMUP_getInstance() {
    static_init();
    return ProblemCode_RUNG_UNDER_WARMUP_instance;
  }
  function ProblemCode_RUNG_WITHOUT_VALUE_getInstance() {
    static_init();
    return ProblemCode_RUNG_WITHOUT_VALUE_instance;
  }
  function ProblemCode_RUN_MIXES_KINDS_getInstance() {
    static_init();
    return ProblemCode_RUN_MIXES_KINDS_instance;
  }
  function ProblemCode_RUNG_CARRIES_TARGETS_getInstance() {
    static_init();
    return ProblemCode_RUNG_CARRIES_TARGETS_instance;
  }
  function get_json() {
    _init_properties_ProgramJson_kt__8o3jdm();
    return json;
  }
  var json;
  function programFormatVersion() {
    _init_properties_ProgramJson_kt__8o3jdm();
    return 6;
  }
  function validateJson(documentJson) {
    _init_properties_ProgramJson_kt__8o3jdm();
    var tmp;
    try {
      // Inline function 'kotlin.collections.map' call
      var this_0 = validate(ProgramDocumentCodec_getInstance().v1b(documentJson));
      // Inline function 'kotlin.collections.mapTo' call
      var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
      var _iterator__ex2g4s = this_0.r();
      while (_iterator__ex2g4s.s()) {
        var item = _iterator__ex2g4s.t();
        var tmp$ret$2 = toOut(item);
        destination.w(tmp$ret$2);
      }
      tmp = destination;
    } catch ($p) {
      var tmp_0;
      if ($p instanceof ProgramFormatException) {
        var refused = $p;
        tmp_0 = listOf(toOut(refused.r1b_1));
      } else {
        throw $p;
      }
      tmp = tmp_0;
    }
    var problems = tmp;
    var tmp0 = get_json();
    // Inline function 'kotlinx.serialization.json.Json.encodeToString' call
    var value = new ValidateOut(problems);
    // Inline function 'kotlinx.serialization.serializer' call
    var this_1 = tmp0.fj();
    // Inline function 'kotlinx.serialization.internal.cast' call
    var this_2 = serializer(this_1, createKType(getKClass(ValidateOut), arrayOf([]), false));
    var tmp$ret$4 = isInterface(this_2, KSerializer) ? this_2 : THROW_CCE();
    return tmp0.lz(tmp$ret$4, value);
  }
  function planJson(snapshotJson, editJson) {
    _init_properties_ProgramJson_kt__8o3jdm();
    var tmp;
    try {
      tmp = plan(ProgramDocumentCodec_getInstance().v1b(snapshotJson), ProgramDocumentCodec_getInstance().v1b(editJson));
    } catch ($p) {
      var tmp_0;
      if ($p instanceof ProgramFormatException) {
        var refused = $p;
        var tmp0 = get_json();
        // Inline function 'kotlinx.serialization.json.Json.encodeToString' call
        var value = new PlanOut(false, VOID, VOID, listOf(toOut(refused.r1b_1)));
        // Inline function 'kotlinx.serialization.serializer' call
        var this_0 = tmp0.fj();
        // Inline function 'kotlinx.serialization.internal.cast' call
        var this_1 = serializer(this_0, createKType(getKClass(PlanOut), arrayOf([]), false));
        var tmp$ret$1 = isInterface(this_1, KSerializer) ? this_1 : THROW_CCE();
        return tmp0.lz(tmp$ret$1, value);
      } else {
        throw $p;
      }
    }
    var result = tmp;
    var tmp_1;
    if (result instanceof Refused) {
      var tmp0_0 = get_json();
      // Inline function 'kotlin.collections.map' call
      var this_2 = result.f1c_1;
      // Inline function 'kotlin.collections.mapTo' call
      var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_2, 10));
      var _iterator__ex2g4s = this_2.r();
      while (_iterator__ex2g4s.s()) {
        var item = _iterator__ex2g4s.t();
        var tmp$ret$5 = toOut(item);
        destination.w(tmp$ret$5);
      }
      // Inline function 'kotlinx.serialization.json.Json.encodeToString' call
      var value_0 = new PlanOut(false, VOID, VOID, destination);
      // Inline function 'kotlinx.serialization.serializer' call
      var this_3 = tmp0_0.fj();
      // Inline function 'kotlinx.serialization.internal.cast' call
      var this_4 = serializer(this_3, createKType(getKClass(PlanOut), arrayOf([]), false));
      var tmp$ret$7 = isInterface(this_4, KSerializer) ? this_4 : THROW_CCE();
      tmp_1 = tmp0_0.lz(tmp$ret$7, value_0);
    } else {
      if (result instanceof Ready) {
        var tmp0_1 = get_json();
        // Inline function 'kotlin.collections.map' call
        var this_5 = result.w1b_1;
        // Inline function 'kotlin.collections.mapTo' call
        var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(this_5, 10));
        var _iterator__ex2g4s_0 = this_5.r();
        while (_iterator__ex2g4s_0.s()) {
          var item_0 = _iterator__ex2g4s_0.t();
          var tmp$ret$11 = new ChangeOut(item_0.y1b_1.j2_1, item_0.z1b_1, item_0.a1c_1, item_0.b1c_1);
          destination_0.w(tmp$ret$11);
        }
        var tmp_2 = destination_0;
        // Inline function 'kotlin.collections.map' call
        var this_6 = result.x1b_1;
        // Inline function 'kotlin.collections.mapTo' call
        var destination_1 = ArrayList_init_$Create$(collectionSizeOrDefault(this_6, 10));
        var _iterator__ex2g4s_1 = this_6.r();
        while (_iterator__ex2g4s_1.s()) {
          var item_1 = _iterator__ex2g4s_1.t();
          var tmp$ret$14 = new WarningOut(item_1.c1c_1.j2_1, item_1.d1c_1, item_1.e1c_1);
          destination_1.w(tmp$ret$14);
        }
        // Inline function 'kotlinx.serialization.json.Json.encodeToString' call
        var value_1 = new PlanOut(true, tmp_2, destination_1);
        // Inline function 'kotlinx.serialization.serializer' call
        var this_7 = tmp0_1.fj();
        // Inline function 'kotlinx.serialization.internal.cast' call
        var this_8 = serializer(this_7, createKType(getKClass(PlanOut), arrayOf([]), false));
        var tmp$ret$16 = isInterface(this_8, KSerializer) ? this_8 : THROW_CCE();
        tmp_1 = tmp0_1.lz(tmp$ret$16, value_1);
      } else {
        noWhenBranchMatchedException();
      }
    }
    return tmp_1;
  }
  function Companion() {
  }
  var Companion_instance_0;
  function Companion_getInstance() {
    return Companion_instance_0;
  }
  function $serializer() {
    $serializer_instance = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.ProblemOut', this, 3);
    tmp0_serialDesc.zp('code', false);
    tmp0_serialDesc.zp('path', false);
    tmp0_serialDesc.zp('detail', true);
    this.g1c_1 = tmp0_serialDesc;
  }
  protoOf($serializer).h1c = function (encoder, value) {
    var tmp0_desc = this.g1c_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    tmp1_output.fk(tmp0_desc, 0, value.i1c_1);
    tmp1_output.fk(tmp0_desc, 1, value.j1c_1);
    if (tmp1_output.nk(tmp0_desc, 2) ? true : !(value.k1c_1 == null)) {
      tmp1_output.jk(tmp0_desc, 2, StringSerializer_getInstance(), value.k1c_1);
    }
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer).pf = function (encoder, value) {
    return this.h1c(encoder, value instanceof ProblemOut ? value : THROW_CCE());
  };
  protoOf($serializer).qf = function (decoder) {
    var tmp0_desc = this.g1c_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_input = decoder.qi(tmp0_desc);
    if (tmp7_input.gj()) {
      tmp4_local0 = tmp7_input.aj(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp7_input.aj(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp7_input.ej(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp7_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp7_input.aj(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp7_input.aj(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp7_input.ej(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp7_input.ri(tmp0_desc);
    return ProblemOut_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, null);
  };
  protoOf($serializer).of = function () {
    return this.g1c_1;
  };
  protoOf($serializer).oq = function () {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), get_nullable(StringSerializer_getInstance())];
  };
  var $serializer_instance;
  function $serializer_getInstance() {
    if ($serializer_instance == null)
      new $serializer();
    return $serializer_instance;
  }
  function ProblemOut_init_$Init$(seen0, code, path, detail, serializationConstructorMarker, $this) {
    if (!(3 === (3 & seen0))) {
      throwMissingFieldException(seen0, 3, $serializer_getInstance().g1c_1);
    }
    $this.i1c_1 = code;
    $this.j1c_1 = path;
    if (0 === (seen0 & 4))
      $this.k1c_1 = null;
    else
      $this.k1c_1 = detail;
    return $this;
  }
  function ProblemOut_init_$Create$(seen0, code, path, detail, serializationConstructorMarker) {
    return ProblemOut_init_$Init$(seen0, code, path, detail, serializationConstructorMarker, objectCreate(protoOf(ProblemOut)));
  }
  function ProblemOut(code, path, detail) {
    detail = detail === VOID ? null : detail;
    this.i1c_1 = code;
    this.j1c_1 = path;
    this.k1c_1 = detail;
  }
  protoOf(ProblemOut).toString = function () {
    return 'ProblemOut(code=' + this.i1c_1 + ', path=' + this.j1c_1 + ', detail=' + this.k1c_1 + ')';
  };
  protoOf(ProblemOut).hashCode = function () {
    var result = getStringHashCode(this.i1c_1);
    result = imul(result, 31) + getStringHashCode(this.j1c_1) | 0;
    result = imul(result, 31) + (this.k1c_1 == null ? 0 : getStringHashCode(this.k1c_1)) | 0;
    return result;
  };
  protoOf(ProblemOut).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ProblemOut))
      return false;
    if (!(this.i1c_1 === other.i1c_1))
      return false;
    if (!(this.j1c_1 === other.j1c_1))
      return false;
    if (!(this.k1c_1 == other.k1c_1))
      return false;
    return true;
  };
  function Companion_0() {
  }
  var Companion_instance_1;
  function Companion_getInstance_0() {
    return Companion_instance_1;
  }
  function $serializer_0() {
    $serializer_instance_0 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.ChangeOut', this, 4);
    tmp0_serialDesc.zp('kind', false);
    tmp0_serialDesc.zp('id', false);
    tmp0_serialDesc.zp('name', true);
    tmp0_serialDesc.zp('detail', true);
    this.l1c_1 = tmp0_serialDesc;
  }
  protoOf($serializer_0).m1c = function (encoder, value) {
    var tmp0_desc = this.l1c_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    tmp1_output.fk(tmp0_desc, 0, value.n1c_1);
    tmp1_output.fk(tmp0_desc, 1, value.o1c_1);
    if (tmp1_output.nk(tmp0_desc, 2) ? true : !(value.p1c_1 == null)) {
      tmp1_output.jk(tmp0_desc, 2, StringSerializer_getInstance(), value.p1c_1);
    }
    if (tmp1_output.nk(tmp0_desc, 3) ? true : !(value.q1c_1 == null)) {
      tmp1_output.jk(tmp0_desc, 3, StringSerializer_getInstance(), value.q1c_1);
    }
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_0).pf = function (encoder, value) {
    return this.m1c(encoder, value instanceof ChangeOut ? value : THROW_CCE());
  };
  protoOf($serializer_0).qf = function (decoder) {
    var tmp0_desc = this.l1c_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_local3 = null;
    var tmp8_input = decoder.qi(tmp0_desc);
    if (tmp8_input.gj()) {
      tmp4_local0 = tmp8_input.aj(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp8_input.aj(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp8_input.ej(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp8_input.ej(tmp0_desc, 3, StringSerializer_getInstance(), tmp7_local3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp8_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp8_input.aj(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp8_input.aj(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp8_input.ej(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp8_input.ej(tmp0_desc, 3, StringSerializer_getInstance(), tmp7_local3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp8_input.ri(tmp0_desc);
    return ChangeOut_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, null);
  };
  protoOf($serializer_0).of = function () {
    return this.l1c_1;
  };
  protoOf($serializer_0).oq = function () {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), get_nullable(StringSerializer_getInstance()), get_nullable(StringSerializer_getInstance())];
  };
  var $serializer_instance_0;
  function $serializer_getInstance_0() {
    if ($serializer_instance_0 == null)
      new $serializer_0();
    return $serializer_instance_0;
  }
  function ChangeOut_init_$Init$(seen0, kind, id, name, detail, serializationConstructorMarker, $this) {
    if (!(3 === (3 & seen0))) {
      throwMissingFieldException(seen0, 3, $serializer_getInstance_0().l1c_1);
    }
    $this.n1c_1 = kind;
    $this.o1c_1 = id;
    if (0 === (seen0 & 4))
      $this.p1c_1 = null;
    else
      $this.p1c_1 = name;
    if (0 === (seen0 & 8))
      $this.q1c_1 = null;
    else
      $this.q1c_1 = detail;
    return $this;
  }
  function ChangeOut_init_$Create$(seen0, kind, id, name, detail, serializationConstructorMarker) {
    return ChangeOut_init_$Init$(seen0, kind, id, name, detail, serializationConstructorMarker, objectCreate(protoOf(ChangeOut)));
  }
  function ChangeOut(kind, id, name, detail) {
    name = name === VOID ? null : name;
    detail = detail === VOID ? null : detail;
    this.n1c_1 = kind;
    this.o1c_1 = id;
    this.p1c_1 = name;
    this.q1c_1 = detail;
  }
  protoOf(ChangeOut).toString = function () {
    return 'ChangeOut(kind=' + this.n1c_1 + ', id=' + this.o1c_1 + ', name=' + this.p1c_1 + ', detail=' + this.q1c_1 + ')';
  };
  protoOf(ChangeOut).hashCode = function () {
    var result = getStringHashCode(this.n1c_1);
    result = imul(result, 31) + getStringHashCode(this.o1c_1) | 0;
    result = imul(result, 31) + (this.p1c_1 == null ? 0 : getStringHashCode(this.p1c_1)) | 0;
    result = imul(result, 31) + (this.q1c_1 == null ? 0 : getStringHashCode(this.q1c_1)) | 0;
    return result;
  };
  protoOf(ChangeOut).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ChangeOut))
      return false;
    if (!(this.n1c_1 === other.n1c_1))
      return false;
    if (!(this.o1c_1 === other.o1c_1))
      return false;
    if (!(this.p1c_1 == other.p1c_1))
      return false;
    if (!(this.q1c_1 == other.q1c_1))
      return false;
    return true;
  };
  function Companion_1() {
  }
  var Companion_instance_2;
  function Companion_getInstance_1() {
    return Companion_instance_2;
  }
  function $serializer_1() {
    $serializer_instance_1 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.WarningOut', this, 3);
    tmp0_serialDesc.zp('code', false);
    tmp0_serialDesc.zp('id', true);
    tmp0_serialDesc.zp('name', true);
    this.r1c_1 = tmp0_serialDesc;
  }
  protoOf($serializer_1).s1c = function (encoder, value) {
    var tmp0_desc = this.r1c_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    tmp1_output.fk(tmp0_desc, 0, value.t1c_1);
    if (tmp1_output.nk(tmp0_desc, 1) ? true : !(value.u1c_1 == null)) {
      tmp1_output.jk(tmp0_desc, 1, StringSerializer_getInstance(), value.u1c_1);
    }
    if (tmp1_output.nk(tmp0_desc, 2) ? true : !(value.v1c_1 == null)) {
      tmp1_output.jk(tmp0_desc, 2, StringSerializer_getInstance(), value.v1c_1);
    }
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_1).pf = function (encoder, value) {
    return this.s1c(encoder, value instanceof WarningOut ? value : THROW_CCE());
  };
  protoOf($serializer_1).qf = function (decoder) {
    var tmp0_desc = this.r1c_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_input = decoder.qi(tmp0_desc);
    if (tmp7_input.gj()) {
      tmp4_local0 = tmp7_input.aj(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp7_input.ej(tmp0_desc, 1, StringSerializer_getInstance(), tmp5_local1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp7_input.ej(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp7_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp7_input.aj(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp7_input.ej(tmp0_desc, 1, StringSerializer_getInstance(), tmp5_local1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp7_input.ej(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp7_input.ri(tmp0_desc);
    return WarningOut_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, null);
  };
  protoOf($serializer_1).of = function () {
    return this.r1c_1;
  };
  protoOf($serializer_1).oq = function () {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), get_nullable(StringSerializer_getInstance()), get_nullable(StringSerializer_getInstance())];
  };
  var $serializer_instance_1;
  function $serializer_getInstance_1() {
    if ($serializer_instance_1 == null)
      new $serializer_1();
    return $serializer_instance_1;
  }
  function WarningOut_init_$Init$(seen0, code, id, name, serializationConstructorMarker, $this) {
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance_1().r1c_1);
    }
    $this.t1c_1 = code;
    if (0 === (seen0 & 2))
      $this.u1c_1 = null;
    else
      $this.u1c_1 = id;
    if (0 === (seen0 & 4))
      $this.v1c_1 = null;
    else
      $this.v1c_1 = name;
    return $this;
  }
  function WarningOut_init_$Create$(seen0, code, id, name, serializationConstructorMarker) {
    return WarningOut_init_$Init$(seen0, code, id, name, serializationConstructorMarker, objectCreate(protoOf(WarningOut)));
  }
  function WarningOut(code, id, name) {
    id = id === VOID ? null : id;
    name = name === VOID ? null : name;
    this.t1c_1 = code;
    this.u1c_1 = id;
    this.v1c_1 = name;
  }
  protoOf(WarningOut).toString = function () {
    return 'WarningOut(code=' + this.t1c_1 + ', id=' + this.u1c_1 + ', name=' + this.v1c_1 + ')';
  };
  protoOf(WarningOut).hashCode = function () {
    var result = getStringHashCode(this.t1c_1);
    result = imul(result, 31) + (this.u1c_1 == null ? 0 : getStringHashCode(this.u1c_1)) | 0;
    result = imul(result, 31) + (this.v1c_1 == null ? 0 : getStringHashCode(this.v1c_1)) | 0;
    return result;
  };
  protoOf(WarningOut).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof WarningOut))
      return false;
    if (!(this.t1c_1 === other.t1c_1))
      return false;
    if (!(this.u1c_1 == other.u1c_1))
      return false;
    if (!(this.v1c_1 == other.v1c_1))
      return false;
    return true;
  };
  function ValidateOut$Companion$$childSerializers$_anonymous__swzrou() {
    return new ArrayListSerializer($serializer_getInstance());
  }
  function Companion_2() {
    Companion_instance_3 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.w1c_1 = [lazy(tmp_0, ValidateOut$Companion$$childSerializers$_anonymous__swzrou)];
  }
  var Companion_instance_3;
  function Companion_getInstance_2() {
    if (Companion_instance_3 == null)
      new Companion_2();
    return Companion_instance_3;
  }
  function $serializer_2() {
    $serializer_instance_2 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.ValidateOut', this, 1);
    tmp0_serialDesc.zp('problems', false);
    this.x1c_1 = tmp0_serialDesc;
  }
  protoOf($serializer_2).y1c = function (encoder, value) {
    var tmp0_desc = this.x1c_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    var tmp2_cached = Companion_getInstance_2().w1c_1;
    tmp1_output.hk(tmp0_desc, 0, tmp2_cached[0].y1(), value.z1c_1);
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_2).pf = function (encoder, value) {
    return this.y1c(encoder, value instanceof ValidateOut ? value : THROW_CCE());
  };
  protoOf($serializer_2).qf = function (decoder) {
    var tmp0_desc = this.x1c_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_input = decoder.qi(tmp0_desc);
    var tmp6_cached = Companion_getInstance_2().w1c_1;
    if (tmp5_input.gj()) {
      tmp4_local0 = tmp5_input.cj(tmp0_desc, 0, tmp6_cached[0].y1(), tmp4_local0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp5_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp5_input.cj(tmp0_desc, 0, tmp6_cached[0].y1(), tmp4_local0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp5_input.ri(tmp0_desc);
    return ValidateOut_init_$Create$(tmp3_bitMask0, tmp4_local0, null);
  };
  protoOf($serializer_2).of = function () {
    return this.x1c_1;
  };
  protoOf($serializer_2).oq = function () {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [Companion_getInstance_2().w1c_1[0].y1()];
  };
  var $serializer_instance_2;
  function $serializer_getInstance_2() {
    if ($serializer_instance_2 == null)
      new $serializer_2();
    return $serializer_instance_2;
  }
  function ValidateOut_init_$Init$(seen0, problems, serializationConstructorMarker, $this) {
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance_2().x1c_1);
    }
    $this.z1c_1 = problems;
    return $this;
  }
  function ValidateOut_init_$Create$(seen0, problems, serializationConstructorMarker) {
    return ValidateOut_init_$Init$(seen0, problems, serializationConstructorMarker, objectCreate(protoOf(ValidateOut)));
  }
  function ValidateOut(problems) {
    Companion_getInstance_2();
    this.z1c_1 = problems;
  }
  protoOf(ValidateOut).toString = function () {
    return 'ValidateOut(problems=' + toString(this.z1c_1) + ')';
  };
  protoOf(ValidateOut).hashCode = function () {
    return hashCode(this.z1c_1);
  };
  protoOf(ValidateOut).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ValidateOut))
      return false;
    if (!equals(this.z1c_1, other.z1c_1))
      return false;
    return true;
  };
  function PlanOut$Companion$$childSerializers$_anonymous__dlb285() {
    return new ArrayListSerializer($serializer_getInstance_0());
  }
  function PlanOut$Companion$$childSerializers$_anonymous__dlb285_0() {
    return new ArrayListSerializer($serializer_getInstance_1());
  }
  function PlanOut$Companion$$childSerializers$_anonymous__dlb285_1() {
    return new ArrayListSerializer($serializer_getInstance());
  }
  function Companion_3() {
    Companion_instance_4 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    var tmp_1 = lazy(tmp_0, PlanOut$Companion$$childSerializers$_anonymous__dlb285);
    var tmp_2 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    var tmp_3 = lazy(tmp_2, PlanOut$Companion$$childSerializers$_anonymous__dlb285_0);
    var tmp_4 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.a1d_1 = [null, tmp_1, tmp_3, lazy(tmp_4, PlanOut$Companion$$childSerializers$_anonymous__dlb285_1)];
  }
  var Companion_instance_4;
  function Companion_getInstance_3() {
    if (Companion_instance_4 == null)
      new Companion_3();
    return Companion_instance_4;
  }
  function $serializer_3() {
    $serializer_instance_3 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.PlanOut', this, 4);
    tmp0_serialDesc.zp('ok', false);
    tmp0_serialDesc.zp('changes', true);
    tmp0_serialDesc.zp('warnings', true);
    tmp0_serialDesc.zp('problems', true);
    this.b1d_1 = tmp0_serialDesc;
  }
  protoOf($serializer_3).c1d = function (encoder, value) {
    var tmp0_desc = this.b1d_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    var tmp2_cached = Companion_getInstance_3().a1d_1;
    tmp1_output.xj(tmp0_desc, 0, value.d1d_1);
    if (tmp1_output.nk(tmp0_desc, 1) ? true : !equals(value.e1d_1, emptyList())) {
      tmp1_output.hk(tmp0_desc, 1, tmp2_cached[1].y1(), value.e1d_1);
    }
    if (tmp1_output.nk(tmp0_desc, 2) ? true : !equals(value.f1d_1, emptyList())) {
      tmp1_output.hk(tmp0_desc, 2, tmp2_cached[2].y1(), value.f1d_1);
    }
    if (tmp1_output.nk(tmp0_desc, 3) ? true : !equals(value.g1d_1, emptyList())) {
      tmp1_output.hk(tmp0_desc, 3, tmp2_cached[3].y1(), value.g1d_1);
    }
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_3).pf = function (encoder, value) {
    return this.c1d(encoder, value instanceof PlanOut ? value : THROW_CCE());
  };
  protoOf($serializer_3).qf = function (decoder) {
    var tmp0_desc = this.b1d_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = false;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_local3 = null;
    var tmp8_input = decoder.qi(tmp0_desc);
    var tmp9_cached = Companion_getInstance_3().a1d_1;
    if (tmp8_input.gj()) {
      tmp4_local0 = tmp8_input.si(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp8_input.cj(tmp0_desc, 1, tmp9_cached[1].y1(), tmp5_local1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp8_input.cj(tmp0_desc, 2, tmp9_cached[2].y1(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp8_input.cj(tmp0_desc, 3, tmp9_cached[3].y1(), tmp7_local3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp8_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp8_input.si(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp8_input.cj(tmp0_desc, 1, tmp9_cached[1].y1(), tmp5_local1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp8_input.cj(tmp0_desc, 2, tmp9_cached[2].y1(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp8_input.cj(tmp0_desc, 3, tmp9_cached[3].y1(), tmp7_local3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp8_input.ri(tmp0_desc);
    return PlanOut_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, null);
  };
  protoOf($serializer_3).of = function () {
    return this.b1d_1;
  };
  protoOf($serializer_3).oq = function () {
    var tmp0_cached = Companion_getInstance_3().a1d_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [BooleanSerializer_getInstance(), tmp0_cached[1].y1(), tmp0_cached[2].y1(), tmp0_cached[3].y1()];
  };
  var $serializer_instance_3;
  function $serializer_getInstance_3() {
    if ($serializer_instance_3 == null)
      new $serializer_3();
    return $serializer_instance_3;
  }
  function PlanOut_init_$Init$(seen0, ok, changes, warnings, problems, serializationConstructorMarker, $this) {
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance_3().b1d_1);
    }
    $this.d1d_1 = ok;
    if (0 === (seen0 & 2))
      $this.e1d_1 = emptyList();
    else
      $this.e1d_1 = changes;
    if (0 === (seen0 & 4))
      $this.f1d_1 = emptyList();
    else
      $this.f1d_1 = warnings;
    if (0 === (seen0 & 8))
      $this.g1d_1 = emptyList();
    else
      $this.g1d_1 = problems;
    return $this;
  }
  function PlanOut_init_$Create$(seen0, ok, changes, warnings, problems, serializationConstructorMarker) {
    return PlanOut_init_$Init$(seen0, ok, changes, warnings, problems, serializationConstructorMarker, objectCreate(protoOf(PlanOut)));
  }
  function PlanOut(ok, changes, warnings, problems) {
    Companion_getInstance_3();
    changes = changes === VOID ? emptyList() : changes;
    warnings = warnings === VOID ? emptyList() : warnings;
    problems = problems === VOID ? emptyList() : problems;
    this.d1d_1 = ok;
    this.e1d_1 = changes;
    this.f1d_1 = warnings;
    this.g1d_1 = problems;
  }
  protoOf(PlanOut).toString = function () {
    return 'PlanOut(ok=' + this.d1d_1 + ', changes=' + toString(this.e1d_1) + ', warnings=' + toString(this.f1d_1) + ', problems=' + toString(this.g1d_1) + ')';
  };
  protoOf(PlanOut).hashCode = function () {
    var result = getBooleanHashCode(this.d1d_1);
    result = imul(result, 31) + hashCode(this.e1d_1) | 0;
    result = imul(result, 31) + hashCode(this.f1d_1) | 0;
    result = imul(result, 31) + hashCode(this.g1d_1) | 0;
    return result;
  };
  protoOf(PlanOut).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof PlanOut))
      return false;
    if (!(this.d1d_1 === other.d1d_1))
      return false;
    if (!equals(this.e1d_1, other.e1d_1))
      return false;
    if (!equals(this.f1d_1, other.f1d_1))
      return false;
    if (!equals(this.g1d_1, other.g1d_1))
      return false;
    return true;
  };
  function toOut(_this__u8e3s4) {
    _init_properties_ProgramJson_kt__8o3jdm();
    return new ProblemOut(_this__u8e3s4.o1b_1.j2_1, _this__u8e3s4.p1b_1, _this__u8e3s4.q1b_1);
  }
  function json$lambda($this$Json) {
    _init_properties_ProgramJson_kt__8o3jdm();
    $this$Json.f10_1 = true;
    $this$Json.h10_1 = true;
    return Unit_instance;
  }
  var properties_initialized_ProgramJson_kt_82zw1w;
  function _init_properties_ProgramJson_kt__8o3jdm() {
    if (!properties_initialized_ProgramJson_kt_82zw1w) {
      properties_initialized_ProgramJson_kt_82zw1w = true;
      json = Json(VOID, json$lambda);
    }
  }
  function ProgramDocument$Companion$$childSerializers$_anonymous__p8b42d() {
    return new ArrayListSerializer($serializer_getInstance_7());
  }
  function ProgramDocument$Companion$$childSerializers$_anonymous__p8b42d_0() {
    return new ArrayListSerializer($serializer_getInstance_8());
  }
  function ProgramDocument$Companion$$childSerializers$_anonymous__p8b42d_1() {
    return new ArrayListSerializer($serializer_getInstance_9());
  }
  function ProgramDocument$Companion$$childSerializers$_anonymous__p8b42d_2() {
    return new ArrayListSerializer($serializer_getInstance_10());
  }
  function ProgramDocument$Companion$$childSerializers$_anonymous__p8b42d_3() {
    return new ArrayListSerializer($serializer_getInstance_11());
  }
  function Companion_4() {
    Companion_instance_5 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    var tmp_1 = lazy(tmp_0, ProgramDocument$Companion$$childSerializers$_anonymous__p8b42d);
    var tmp_2 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    var tmp_3 = lazy(tmp_2, ProgramDocument$Companion$$childSerializers$_anonymous__p8b42d_0);
    var tmp_4 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    var tmp_5 = lazy(tmp_4, ProgramDocument$Companion$$childSerializers$_anonymous__p8b42d_1);
    var tmp_6 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    var tmp_7 = lazy(tmp_6, ProgramDocument$Companion$$childSerializers$_anonymous__p8b42d_2);
    var tmp_8 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.h1d_1 = [null, null, null, tmp_1, tmp_3, tmp_5, tmp_7, lazy(tmp_8, ProgramDocument$Companion$$childSerializers$_anonymous__p8b42d_3), null];
  }
  var Companion_instance_5;
  function Companion_getInstance_4() {
    if (Companion_instance_5 == null)
      new Companion_4();
    return Companion_instance_5;
  }
  function $serializer_4() {
    $serializer_instance_4 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.ProgramDocument', this, 9);
    tmp0_serialDesc.zp('formatVersion', false);
    tmp0_serialDesc.zp('exportedAt', false);
    tmp0_serialDesc.zp('program', false);
    tmp0_serialDesc.zp('slots', true);
    tmp0_serialDesc.zp('templates', true);
    tmp0_serialDesc.zp('templateExercises', true);
    tmp0_serialDesc.zp('templateSets', true);
    tmp0_serialDesc.zp('exercises', true);
    tmp0_serialDesc.zp('edit', true);
    this.i1d_1 = tmp0_serialDesc;
  }
  protoOf($serializer_4).j1d = function (encoder, value) {
    var tmp0_desc = this.i1d_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    var tmp2_cached = Companion_getInstance_4().h1d_1;
    tmp1_output.ak(tmp0_desc, 0, value.k1d_1);
    tmp1_output.bk(tmp0_desc, 1, value.l1d_1);
    tmp1_output.hk(tmp0_desc, 2, $serializer_getInstance_6(), value.m1d_1);
    if (tmp1_output.nk(tmp0_desc, 3) ? true : !equals(value.n1d_1, emptyList())) {
      tmp1_output.hk(tmp0_desc, 3, tmp2_cached[3].y1(), value.n1d_1);
    }
    if (tmp1_output.nk(tmp0_desc, 4) ? true : !equals(value.o1d_1, emptyList())) {
      tmp1_output.hk(tmp0_desc, 4, tmp2_cached[4].y1(), value.o1d_1);
    }
    if (tmp1_output.nk(tmp0_desc, 5) ? true : !equals(value.p1d_1, emptyList())) {
      tmp1_output.hk(tmp0_desc, 5, tmp2_cached[5].y1(), value.p1d_1);
    }
    if (tmp1_output.nk(tmp0_desc, 6) ? true : !equals(value.q1d_1, emptyList())) {
      tmp1_output.hk(tmp0_desc, 6, tmp2_cached[6].y1(), value.q1d_1);
    }
    if (tmp1_output.nk(tmp0_desc, 7) ? true : !equals(value.r1d_1, emptyList())) {
      tmp1_output.hk(tmp0_desc, 7, tmp2_cached[7].y1(), value.r1d_1);
    }
    if (!(value.s1d_1 == null)) {
      tmp1_output.jk(tmp0_desc, 8, $serializer_getInstance_5(), value.s1d_1);
    }
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_4).pf = function (encoder, value) {
    return this.j1d(encoder, value instanceof ProgramDocument ? value : THROW_CCE());
  };
  protoOf($serializer_4).qf = function (decoder) {
    var tmp0_desc = this.i1d_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = 0;
    var tmp5_local1 = new Long(0, 0);
    var tmp6_local2 = null;
    var tmp7_local3 = null;
    var tmp8_local4 = null;
    var tmp9_local5 = null;
    var tmp10_local6 = null;
    var tmp11_local7 = null;
    var tmp12_local8 = null;
    var tmp13_input = decoder.qi(tmp0_desc);
    var tmp14_cached = Companion_getInstance_4().h1d_1;
    if (tmp13_input.gj()) {
      tmp4_local0 = tmp13_input.vi(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp13_input.wi(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp13_input.cj(tmp0_desc, 2, $serializer_getInstance_6(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp13_input.cj(tmp0_desc, 3, tmp14_cached[3].y1(), tmp7_local3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp13_input.cj(tmp0_desc, 4, tmp14_cached[4].y1(), tmp8_local4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp13_input.cj(tmp0_desc, 5, tmp14_cached[5].y1(), tmp9_local5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp13_input.cj(tmp0_desc, 6, tmp14_cached[6].y1(), tmp10_local6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
      tmp11_local7 = tmp13_input.cj(tmp0_desc, 7, tmp14_cached[7].y1(), tmp11_local7);
      tmp3_bitMask0 = tmp3_bitMask0 | 128;
      tmp12_local8 = tmp13_input.ej(tmp0_desc, 8, $serializer_getInstance_5(), tmp12_local8);
      tmp3_bitMask0 = tmp3_bitMask0 | 256;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp13_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp13_input.vi(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp13_input.wi(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp13_input.cj(tmp0_desc, 2, $serializer_getInstance_6(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp13_input.cj(tmp0_desc, 3, tmp14_cached[3].y1(), tmp7_local3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp13_input.cj(tmp0_desc, 4, tmp14_cached[4].y1(), tmp8_local4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp13_input.cj(tmp0_desc, 5, tmp14_cached[5].y1(), tmp9_local5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp13_input.cj(tmp0_desc, 6, tmp14_cached[6].y1(), tmp10_local6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          case 7:
            tmp11_local7 = tmp13_input.cj(tmp0_desc, 7, tmp14_cached[7].y1(), tmp11_local7);
            tmp3_bitMask0 = tmp3_bitMask0 | 128;
            break;
          case 8:
            tmp12_local8 = tmp13_input.ej(tmp0_desc, 8, $serializer_getInstance_5(), tmp12_local8);
            tmp3_bitMask0 = tmp3_bitMask0 | 256;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp13_input.ri(tmp0_desc);
    return ProgramDocument_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, tmp11_local7, tmp12_local8, null);
  };
  protoOf($serializer_4).of = function () {
    return this.i1d_1;
  };
  protoOf($serializer_4).oq = function () {
    var tmp0_cached = Companion_getInstance_4().h1d_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [IntSerializer_getInstance(), LongSerializer_getInstance(), $serializer_getInstance_6(), tmp0_cached[3].y1(), tmp0_cached[4].y1(), tmp0_cached[5].y1(), tmp0_cached[6].y1(), tmp0_cached[7].y1(), get_nullable($serializer_getInstance_5())];
  };
  var $serializer_instance_4;
  function $serializer_getInstance_4() {
    if ($serializer_instance_4 == null)
      new $serializer_4();
    return $serializer_instance_4;
  }
  function ProgramDocument_init_$Init$(seen0, formatVersion, exportedAt, program, slots, templates, templateExercises, templateSets, exercises, edit, serializationConstructorMarker, $this) {
    if (!(7 === (7 & seen0))) {
      throwMissingFieldException(seen0, 7, $serializer_getInstance_4().i1d_1);
    }
    $this.k1d_1 = formatVersion;
    $this.l1d_1 = exportedAt;
    $this.m1d_1 = program;
    if (0 === (seen0 & 8))
      $this.n1d_1 = emptyList();
    else
      $this.n1d_1 = slots;
    if (0 === (seen0 & 16))
      $this.o1d_1 = emptyList();
    else
      $this.o1d_1 = templates;
    if (0 === (seen0 & 32))
      $this.p1d_1 = emptyList();
    else
      $this.p1d_1 = templateExercises;
    if (0 === (seen0 & 64))
      $this.q1d_1 = emptyList();
    else
      $this.q1d_1 = templateSets;
    if (0 === (seen0 & 128))
      $this.r1d_1 = emptyList();
    else
      $this.r1d_1 = exercises;
    if (0 === (seen0 & 256))
      $this.s1d_1 = null;
    else
      $this.s1d_1 = edit;
    return $this;
  }
  function ProgramDocument_init_$Create$(seen0, formatVersion, exportedAt, program, slots, templates, templateExercises, templateSets, exercises, edit, serializationConstructorMarker) {
    return ProgramDocument_init_$Init$(seen0, formatVersion, exportedAt, program, slots, templates, templateExercises, templateSets, exercises, edit, serializationConstructorMarker, objectCreate(protoOf(ProgramDocument)));
  }
  function ProgramDocument(formatVersion, exportedAt, program, slots, templates, templateExercises, templateSets, exercises, edit) {
    Companion_getInstance_4();
    slots = slots === VOID ? emptyList() : slots;
    templates = templates === VOID ? emptyList() : templates;
    templateExercises = templateExercises === VOID ? emptyList() : templateExercises;
    templateSets = templateSets === VOID ? emptyList() : templateSets;
    exercises = exercises === VOID ? emptyList() : exercises;
    edit = edit === VOID ? null : edit;
    this.k1d_1 = formatVersion;
    this.l1d_1 = exportedAt;
    this.m1d_1 = program;
    this.n1d_1 = slots;
    this.o1d_1 = templates;
    this.p1d_1 = templateExercises;
    this.q1d_1 = templateSets;
    this.r1d_1 = exercises;
    this.s1d_1 = edit;
  }
  protoOf(ProgramDocument).toString = function () {
    return 'ProgramDocument(formatVersion=' + this.k1d_1 + ', exportedAt=' + this.l1d_1.toString() + ', program=' + this.m1d_1.toString() + ', slots=' + toString(this.n1d_1) + ', templates=' + toString(this.o1d_1) + ', templateExercises=' + toString(this.p1d_1) + ', templateSets=' + toString(this.q1d_1) + ', exercises=' + toString(this.r1d_1) + ', edit=' + toString_0(this.s1d_1) + ')';
  };
  protoOf(ProgramDocument).hashCode = function () {
    var result = this.k1d_1;
    result = imul(result, 31) + this.l1d_1.hashCode() | 0;
    result = imul(result, 31) + this.m1d_1.hashCode() | 0;
    result = imul(result, 31) + hashCode(this.n1d_1) | 0;
    result = imul(result, 31) + hashCode(this.o1d_1) | 0;
    result = imul(result, 31) + hashCode(this.p1d_1) | 0;
    result = imul(result, 31) + hashCode(this.q1d_1) | 0;
    result = imul(result, 31) + hashCode(this.r1d_1) | 0;
    result = imul(result, 31) + (this.s1d_1 == null ? 0 : this.s1d_1.hashCode()) | 0;
    return result;
  };
  protoOf(ProgramDocument).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ProgramDocument))
      return false;
    if (!(this.k1d_1 === other.k1d_1))
      return false;
    if (!equalsLong(this.l1d_1, other.l1d_1))
      return false;
    if (!this.m1d_1.equals(other.m1d_1))
      return false;
    if (!equals(this.n1d_1, other.n1d_1))
      return false;
    if (!equals(this.o1d_1, other.o1d_1))
      return false;
    if (!equals(this.p1d_1, other.p1d_1))
      return false;
    if (!equals(this.q1d_1, other.q1d_1))
      return false;
    if (!equals(this.r1d_1, other.r1d_1))
      return false;
    if (!equals(this.s1d_1, other.s1d_1))
      return false;
    return true;
  };
  function $serializer_5() {
    $serializer_instance_5 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.EditMarker', this, 1);
    tmp0_serialDesc.zp('baseExportedAt', false);
    this.t1d_1 = tmp0_serialDesc;
  }
  protoOf($serializer_5).u1d = function (encoder, value) {
    var tmp0_desc = this.t1d_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    tmp1_output.bk(tmp0_desc, 0, value.v1d_1);
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_5).pf = function (encoder, value) {
    return this.u1d(encoder, value instanceof EditMarker ? value : THROW_CCE());
  };
  protoOf($serializer_5).qf = function (decoder) {
    var tmp0_desc = this.t1d_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = new Long(0, 0);
    var tmp5_input = decoder.qi(tmp0_desc);
    if (tmp5_input.gj()) {
      tmp4_local0 = tmp5_input.wi(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp5_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp5_input.wi(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp5_input.ri(tmp0_desc);
    return EditMarker_init_$Create$(tmp3_bitMask0, tmp4_local0, null);
  };
  protoOf($serializer_5).of = function () {
    return this.t1d_1;
  };
  protoOf($serializer_5).oq = function () {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [LongSerializer_getInstance()];
  };
  var $serializer_instance_5;
  function $serializer_getInstance_5() {
    if ($serializer_instance_5 == null)
      new $serializer_5();
    return $serializer_instance_5;
  }
  function EditMarker_init_$Init$(seen0, baseExportedAt, serializationConstructorMarker, $this) {
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance_5().t1d_1);
    }
    $this.v1d_1 = baseExportedAt;
    return $this;
  }
  function EditMarker_init_$Create$(seen0, baseExportedAt, serializationConstructorMarker) {
    return EditMarker_init_$Init$(seen0, baseExportedAt, serializationConstructorMarker, objectCreate(protoOf(EditMarker)));
  }
  function EditMarker() {
  }
  protoOf(EditMarker).toString = function () {
    return 'EditMarker(baseExportedAt=' + this.v1d_1.toString() + ')';
  };
  protoOf(EditMarker).hashCode = function () {
    return this.v1d_1.hashCode();
  };
  protoOf(EditMarker).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof EditMarker))
      return false;
    if (!equalsLong(this.v1d_1, other.v1d_1))
      return false;
    return true;
  };
  function ProgramDto$Companion$$childSerializers$_anonymous__lqadm7() {
    return Companion_getInstance_6().b12();
  }
  function Companion_5() {
    Companion_instance_6 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.x1d_1 = [null, null, null, null, lazy(tmp_0, ProgramDto$Companion$$childSerializers$_anonymous__lqadm7), null, null, null, null];
  }
  var Companion_instance_6;
  function Companion_getInstance_5() {
    if (Companion_instance_6 == null)
      new Companion_5();
    return Companion_instance_6;
  }
  function $serializer_6() {
    $serializer_instance_6 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.ProgramDto', this, 9);
    tmp0_serialDesc.zp('id', false);
    tmp0_serialDesc.zp('name', false);
    tmp0_serialDesc.zp('isActive', true);
    tmp0_serialDesc.zp('position', true);
    tmp0_serialDesc.zp('kind', true);
    tmp0_serialDesc.zp('startDate', true);
    tmp0_serialDesc.zp('createdAt', false);
    tmp0_serialDesc.zp('updatedAt', false);
    tmp0_serialDesc.zp('deletedAt', true);
    this.y1d_1 = tmp0_serialDesc;
  }
  protoOf($serializer_6).z1d = function (encoder, value) {
    var tmp0_desc = this.y1d_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    var tmp2_cached = Companion_getInstance_5().x1d_1;
    tmp1_output.fk(tmp0_desc, 0, value.a1e_1);
    tmp1_output.fk(tmp0_desc, 1, value.b1e_1);
    if (tmp1_output.nk(tmp0_desc, 2) ? true : !(value.c1e_1 === false)) {
      tmp1_output.xj(tmp0_desc, 2, value.c1e_1);
    }
    if (tmp1_output.nk(tmp0_desc, 3) ? true : !(value.d1e_1 === 0)) {
      tmp1_output.ak(tmp0_desc, 3, value.d1e_1);
    }
    if (tmp1_output.nk(tmp0_desc, 4) ? true : !value.e1e_1.equals(ProgramKind_WEEKLY_getInstance())) {
      tmp1_output.hk(tmp0_desc, 4, tmp2_cached[4].y1(), value.e1e_1);
    }
    if (tmp1_output.nk(tmp0_desc, 5) ? true : !(value.f1e_1 == null)) {
      tmp1_output.jk(tmp0_desc, 5, LongSerializer_getInstance(), value.f1e_1);
    }
    tmp1_output.bk(tmp0_desc, 6, value.g1e_1);
    tmp1_output.bk(tmp0_desc, 7, value.h1e_1);
    if (tmp1_output.nk(tmp0_desc, 8) ? true : !(value.i1e_1 == null)) {
      tmp1_output.jk(tmp0_desc, 8, LongSerializer_getInstance(), value.i1e_1);
    }
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_6).pf = function (encoder, value) {
    return this.z1d(encoder, value instanceof ProgramDto ? value : THROW_CCE());
  };
  protoOf($serializer_6).qf = function (decoder) {
    var tmp0_desc = this.y1d_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = false;
    var tmp7_local3 = 0;
    var tmp8_local4 = null;
    var tmp9_local5 = null;
    var tmp10_local6 = new Long(0, 0);
    var tmp11_local7 = new Long(0, 0);
    var tmp12_local8 = null;
    var tmp13_input = decoder.qi(tmp0_desc);
    var tmp14_cached = Companion_getInstance_5().x1d_1;
    if (tmp13_input.gj()) {
      tmp4_local0 = tmp13_input.aj(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp13_input.aj(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp13_input.si(tmp0_desc, 2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp13_input.vi(tmp0_desc, 3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp13_input.cj(tmp0_desc, 4, tmp14_cached[4].y1(), tmp8_local4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp13_input.ej(tmp0_desc, 5, LongSerializer_getInstance(), tmp9_local5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp13_input.wi(tmp0_desc, 6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
      tmp11_local7 = tmp13_input.wi(tmp0_desc, 7);
      tmp3_bitMask0 = tmp3_bitMask0 | 128;
      tmp12_local8 = tmp13_input.ej(tmp0_desc, 8, LongSerializer_getInstance(), tmp12_local8);
      tmp3_bitMask0 = tmp3_bitMask0 | 256;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp13_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp13_input.aj(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp13_input.aj(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp13_input.si(tmp0_desc, 2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp13_input.vi(tmp0_desc, 3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp13_input.cj(tmp0_desc, 4, tmp14_cached[4].y1(), tmp8_local4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp13_input.ej(tmp0_desc, 5, LongSerializer_getInstance(), tmp9_local5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp13_input.wi(tmp0_desc, 6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          case 7:
            tmp11_local7 = tmp13_input.wi(tmp0_desc, 7);
            tmp3_bitMask0 = tmp3_bitMask0 | 128;
            break;
          case 8:
            tmp12_local8 = tmp13_input.ej(tmp0_desc, 8, LongSerializer_getInstance(), tmp12_local8);
            tmp3_bitMask0 = tmp3_bitMask0 | 256;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp13_input.ri(tmp0_desc);
    return ProgramDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, tmp11_local7, tmp12_local8, null);
  };
  protoOf($serializer_6).of = function () {
    return this.y1d_1;
  };
  protoOf($serializer_6).oq = function () {
    var tmp0_cached = Companion_getInstance_5().x1d_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), BooleanSerializer_getInstance(), IntSerializer_getInstance(), tmp0_cached[4].y1(), get_nullable(LongSerializer_getInstance()), LongSerializer_getInstance(), LongSerializer_getInstance(), get_nullable(LongSerializer_getInstance())];
  };
  var $serializer_instance_6;
  function $serializer_getInstance_6() {
    if ($serializer_instance_6 == null)
      new $serializer_6();
    return $serializer_instance_6;
  }
  function ProgramDto_init_$Init$(seen0, id, name, isActive, position, kind, startDate, createdAt, updatedAt, deletedAt, serializationConstructorMarker, $this) {
    if (!(195 === (195 & seen0))) {
      throwMissingFieldException(seen0, 195, $serializer_getInstance_6().y1d_1);
    }
    $this.a1e_1 = id;
    $this.b1e_1 = name;
    if (0 === (seen0 & 4))
      $this.c1e_1 = false;
    else
      $this.c1e_1 = isActive;
    if (0 === (seen0 & 8))
      $this.d1e_1 = 0;
    else
      $this.d1e_1 = position;
    if (0 === (seen0 & 16))
      $this.e1e_1 = ProgramKind_WEEKLY_getInstance();
    else
      $this.e1e_1 = kind;
    if (0 === (seen0 & 32))
      $this.f1e_1 = null;
    else
      $this.f1e_1 = startDate;
    $this.g1e_1 = createdAt;
    $this.h1e_1 = updatedAt;
    if (0 === (seen0 & 256))
      $this.i1e_1 = null;
    else
      $this.i1e_1 = deletedAt;
    return $this;
  }
  function ProgramDto_init_$Create$(seen0, id, name, isActive, position, kind, startDate, createdAt, updatedAt, deletedAt, serializationConstructorMarker) {
    return ProgramDto_init_$Init$(seen0, id, name, isActive, position, kind, startDate, createdAt, updatedAt, deletedAt, serializationConstructorMarker, objectCreate(protoOf(ProgramDto)));
  }
  function ProgramDto() {
  }
  protoOf(ProgramDto).toString = function () {
    return 'ProgramDto(id=' + this.a1e_1 + ', name=' + this.b1e_1 + ', isActive=' + this.c1e_1 + ', position=' + this.d1e_1 + ', kind=' + this.e1e_1.toString() + ', startDate=' + toString_0(this.f1e_1) + ', createdAt=' + this.g1e_1.toString() + ', updatedAt=' + this.h1e_1.toString() + ', deletedAt=' + toString_0(this.i1e_1) + ')';
  };
  protoOf(ProgramDto).hashCode = function () {
    var result = getStringHashCode(this.a1e_1);
    result = imul(result, 31) + getStringHashCode(this.b1e_1) | 0;
    result = imul(result, 31) + getBooleanHashCode(this.c1e_1) | 0;
    result = imul(result, 31) + this.d1e_1 | 0;
    result = imul(result, 31) + this.e1e_1.hashCode() | 0;
    result = imul(result, 31) + (this.f1e_1 == null ? 0 : this.f1e_1.hashCode()) | 0;
    result = imul(result, 31) + this.g1e_1.hashCode() | 0;
    result = imul(result, 31) + this.h1e_1.hashCode() | 0;
    result = imul(result, 31) + (this.i1e_1 == null ? 0 : this.i1e_1.hashCode()) | 0;
    return result;
  };
  protoOf(ProgramDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ProgramDto))
      return false;
    if (!(this.a1e_1 === other.a1e_1))
      return false;
    if (!(this.b1e_1 === other.b1e_1))
      return false;
    if (!(this.c1e_1 === other.c1e_1))
      return false;
    if (!(this.d1e_1 === other.d1e_1))
      return false;
    if (!this.e1e_1.equals(other.e1e_1))
      return false;
    if (!equals(this.f1e_1, other.f1e_1))
      return false;
    if (!equalsLong(this.g1e_1, other.g1e_1))
      return false;
    if (!equalsLong(this.h1e_1, other.h1e_1))
      return false;
    if (!equals(this.i1e_1, other.i1e_1))
      return false;
    return true;
  };
  function _get_$cachedSerializer__te6jhj($this) {
    return $this.w1d_1.y1();
  }
  function ProgramKind$Companion$_anonymous__fa263t() {
    return createSimpleEnumSerializer('com.example.androidapp.shared.ProgramKind', values());
  }
  var static_init_called_0;
  function static_init_0() {
    if (static_init_called_0)
      return Unit_instance;
    static_init_called_0 = true;
    ProgramKind_WEEKLY_instance = new ProgramKind('WEEKLY', 0);
    ProgramKind_SEQUENCE_instance = new ProgramKind('SEQUENCE', 1);
    if (Companion_instance_7 == null) {
      Companion_instance;
      new Companion_6();
    }
  }
  var ProgramKind_WEEKLY_instance;
  var ProgramKind_SEQUENCE_instance;
  function values() {
    static_init_0();
    return [ProgramKind_WEEKLY_getInstance(), ProgramKind_SEQUENCE_getInstance()];
  }
  function Companion_6() {
    Companion_instance_7 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp.w1d_1 = lazy(tmp_0, ProgramKind$Companion$_anonymous__fa263t);
  }
  protoOf(Companion_6).b12 = function () {
    return _get_$cachedSerializer__te6jhj(this);
  };
  protoOf(Companion_6).br = function (typeParamsSerializers) {
    return this.b12();
  };
  var Companion_instance_7;
  function Companion_getInstance_6() {
    static_init_0();
    return Companion_instance_7;
  }
  function ProgramKind(name, ordinal) {
    Enum.call(this, name, ordinal);
  }
  function ProgramSlotDto$Companion$$childSerializers$_anonymous__iuzt59() {
    return Companion_getInstance_10().b12();
  }
  function Companion_7() {
    Companion_instance_8 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.k1e_1 = [null, null, null, null, lazy(tmp_0, ProgramSlotDto$Companion$$childSerializers$_anonymous__iuzt59), null, null, null];
  }
  var Companion_instance_8;
  function Companion_getInstance_7() {
    if (Companion_instance_8 == null)
      new Companion_7();
    return Companion_instance_8;
  }
  function $serializer_7() {
    $serializer_instance_7 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.ProgramSlotDto', this, 8);
    tmp0_serialDesc.zp('id', false);
    tmp0_serialDesc.zp('programId', false);
    tmp0_serialDesc.zp('templateId', false);
    tmp0_serialDesc.zp('position', false);
    tmp0_serialDesc.zp('weekday', true);
    tmp0_serialDesc.zp('createdAt', false);
    tmp0_serialDesc.zp('updatedAt', false);
    tmp0_serialDesc.zp('deletedAt', true);
    this.l1e_1 = tmp0_serialDesc;
  }
  protoOf($serializer_7).m1e = function (encoder, value) {
    var tmp0_desc = this.l1e_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    var tmp2_cached = Companion_getInstance_7().k1e_1;
    tmp1_output.fk(tmp0_desc, 0, value.n1e_1);
    tmp1_output.fk(tmp0_desc, 1, value.o1e_1);
    tmp1_output.fk(tmp0_desc, 2, value.p1e_1);
    tmp1_output.ak(tmp0_desc, 3, value.q1e_1);
    if (tmp1_output.nk(tmp0_desc, 4) ? true : !(value.r1e_1 == null)) {
      tmp1_output.jk(tmp0_desc, 4, tmp2_cached[4].y1(), value.r1e_1);
    }
    tmp1_output.bk(tmp0_desc, 5, value.s1e_1);
    tmp1_output.bk(tmp0_desc, 6, value.t1e_1);
    if (tmp1_output.nk(tmp0_desc, 7) ? true : !(value.u1e_1 == null)) {
      tmp1_output.jk(tmp0_desc, 7, LongSerializer_getInstance(), value.u1e_1);
    }
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_7).pf = function (encoder, value) {
    return this.m1e(encoder, value instanceof ProgramSlotDto ? value : THROW_CCE());
  };
  protoOf($serializer_7).qf = function (decoder) {
    var tmp0_desc = this.l1e_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_local3 = 0;
    var tmp8_local4 = null;
    var tmp9_local5 = new Long(0, 0);
    var tmp10_local6 = new Long(0, 0);
    var tmp11_local7 = null;
    var tmp12_input = decoder.qi(tmp0_desc);
    var tmp13_cached = Companion_getInstance_7().k1e_1;
    if (tmp12_input.gj()) {
      tmp4_local0 = tmp12_input.aj(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp12_input.aj(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp12_input.aj(tmp0_desc, 2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp12_input.vi(tmp0_desc, 3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp12_input.ej(tmp0_desc, 4, tmp13_cached[4].y1(), tmp8_local4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp12_input.wi(tmp0_desc, 5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp12_input.wi(tmp0_desc, 6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
      tmp11_local7 = tmp12_input.ej(tmp0_desc, 7, LongSerializer_getInstance(), tmp11_local7);
      tmp3_bitMask0 = tmp3_bitMask0 | 128;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp12_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp12_input.aj(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp12_input.aj(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp12_input.aj(tmp0_desc, 2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp12_input.vi(tmp0_desc, 3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp12_input.ej(tmp0_desc, 4, tmp13_cached[4].y1(), tmp8_local4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp12_input.wi(tmp0_desc, 5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp12_input.wi(tmp0_desc, 6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          case 7:
            tmp11_local7 = tmp12_input.ej(tmp0_desc, 7, LongSerializer_getInstance(), tmp11_local7);
            tmp3_bitMask0 = tmp3_bitMask0 | 128;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp12_input.ri(tmp0_desc);
    return ProgramSlotDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, tmp11_local7, null);
  };
  protoOf($serializer_7).of = function () {
    return this.l1e_1;
  };
  protoOf($serializer_7).oq = function () {
    var tmp0_cached = Companion_getInstance_7().k1e_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), StringSerializer_getInstance(), IntSerializer_getInstance(), get_nullable(tmp0_cached[4].y1()), LongSerializer_getInstance(), LongSerializer_getInstance(), get_nullable(LongSerializer_getInstance())];
  };
  var $serializer_instance_7;
  function $serializer_getInstance_7() {
    if ($serializer_instance_7 == null)
      new $serializer_7();
    return $serializer_instance_7;
  }
  function ProgramSlotDto_init_$Init$(seen0, id, programId, templateId, position, weekday, createdAt, updatedAt, deletedAt, serializationConstructorMarker, $this) {
    if (!(111 === (111 & seen0))) {
      throwMissingFieldException(seen0, 111, $serializer_getInstance_7().l1e_1);
    }
    $this.n1e_1 = id;
    $this.o1e_1 = programId;
    $this.p1e_1 = templateId;
    $this.q1e_1 = position;
    if (0 === (seen0 & 16))
      $this.r1e_1 = null;
    else
      $this.r1e_1 = weekday;
    $this.s1e_1 = createdAt;
    $this.t1e_1 = updatedAt;
    if (0 === (seen0 & 128))
      $this.u1e_1 = null;
    else
      $this.u1e_1 = deletedAt;
    return $this;
  }
  function ProgramSlotDto_init_$Create$(seen0, id, programId, templateId, position, weekday, createdAt, updatedAt, deletedAt, serializationConstructorMarker) {
    return ProgramSlotDto_init_$Init$(seen0, id, programId, templateId, position, weekday, createdAt, updatedAt, deletedAt, serializationConstructorMarker, objectCreate(protoOf(ProgramSlotDto)));
  }
  function ProgramSlotDto() {
  }
  protoOf(ProgramSlotDto).toString = function () {
    return 'ProgramSlotDto(id=' + this.n1e_1 + ', programId=' + this.o1e_1 + ', templateId=' + this.p1e_1 + ', position=' + this.q1e_1 + ', weekday=' + toString_0(this.r1e_1) + ', createdAt=' + this.s1e_1.toString() + ', updatedAt=' + this.t1e_1.toString() + ', deletedAt=' + toString_0(this.u1e_1) + ')';
  };
  protoOf(ProgramSlotDto).hashCode = function () {
    var result = getStringHashCode(this.n1e_1);
    result = imul(result, 31) + getStringHashCode(this.o1e_1) | 0;
    result = imul(result, 31) + getStringHashCode(this.p1e_1) | 0;
    result = imul(result, 31) + this.q1e_1 | 0;
    result = imul(result, 31) + (this.r1e_1 == null ? 0 : this.r1e_1.hashCode()) | 0;
    result = imul(result, 31) + this.s1e_1.hashCode() | 0;
    result = imul(result, 31) + this.t1e_1.hashCode() | 0;
    result = imul(result, 31) + (this.u1e_1 == null ? 0 : this.u1e_1.hashCode()) | 0;
    return result;
  };
  protoOf(ProgramSlotDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ProgramSlotDto))
      return false;
    if (!(this.n1e_1 === other.n1e_1))
      return false;
    if (!(this.o1e_1 === other.o1e_1))
      return false;
    if (!(this.p1e_1 === other.p1e_1))
      return false;
    if (!(this.q1e_1 === other.q1e_1))
      return false;
    if (!equals(this.r1e_1, other.r1e_1))
      return false;
    if (!equalsLong(this.s1e_1, other.s1e_1))
      return false;
    if (!equalsLong(this.t1e_1, other.t1e_1))
      return false;
    if (!equals(this.u1e_1, other.u1e_1))
      return false;
    return true;
  };
  function $serializer_8() {
    $serializer_instance_8 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.TemplateDto', this, 6);
    tmp0_serialDesc.zp('id', false);
    tmp0_serialDesc.zp('name', false);
    tmp0_serialDesc.zp('versionOf', true);
    tmp0_serialDesc.zp('createdAt', false);
    tmp0_serialDesc.zp('updatedAt', false);
    tmp0_serialDesc.zp('deletedAt', true);
    this.v1e_1 = tmp0_serialDesc;
  }
  protoOf($serializer_8).w1e = function (encoder, value) {
    var tmp0_desc = this.v1e_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    tmp1_output.fk(tmp0_desc, 0, value.x1e_1);
    tmp1_output.fk(tmp0_desc, 1, value.y1e_1);
    if (tmp1_output.nk(tmp0_desc, 2) ? true : !(value.z1e_1 == null)) {
      tmp1_output.jk(tmp0_desc, 2, StringSerializer_getInstance(), value.z1e_1);
    }
    tmp1_output.bk(tmp0_desc, 3, value.a1f_1);
    tmp1_output.bk(tmp0_desc, 4, value.b1f_1);
    if (tmp1_output.nk(tmp0_desc, 5) ? true : !(value.c1f_1 == null)) {
      tmp1_output.jk(tmp0_desc, 5, LongSerializer_getInstance(), value.c1f_1);
    }
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_8).pf = function (encoder, value) {
    return this.w1e(encoder, value instanceof TemplateDto ? value : THROW_CCE());
  };
  protoOf($serializer_8).qf = function (decoder) {
    var tmp0_desc = this.v1e_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_local3 = new Long(0, 0);
    var tmp8_local4 = new Long(0, 0);
    var tmp9_local5 = null;
    var tmp10_input = decoder.qi(tmp0_desc);
    if (tmp10_input.gj()) {
      tmp4_local0 = tmp10_input.aj(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp10_input.aj(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp10_input.ej(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp10_input.wi(tmp0_desc, 3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp10_input.wi(tmp0_desc, 4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp10_input.ej(tmp0_desc, 5, LongSerializer_getInstance(), tmp9_local5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp10_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp10_input.aj(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp10_input.aj(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp10_input.ej(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp10_input.wi(tmp0_desc, 3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp10_input.wi(tmp0_desc, 4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp10_input.ej(tmp0_desc, 5, LongSerializer_getInstance(), tmp9_local5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp10_input.ri(tmp0_desc);
    return TemplateDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, null);
  };
  protoOf($serializer_8).of = function () {
    return this.v1e_1;
  };
  protoOf($serializer_8).oq = function () {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), get_nullable(StringSerializer_getInstance()), LongSerializer_getInstance(), LongSerializer_getInstance(), get_nullable(LongSerializer_getInstance())];
  };
  var $serializer_instance_8;
  function $serializer_getInstance_8() {
    if ($serializer_instance_8 == null)
      new $serializer_8();
    return $serializer_instance_8;
  }
  function TemplateDto_init_$Init$(seen0, id, name, versionOf, createdAt, updatedAt, deletedAt, serializationConstructorMarker, $this) {
    if (!(27 === (27 & seen0))) {
      throwMissingFieldException(seen0, 27, $serializer_getInstance_8().v1e_1);
    }
    $this.x1e_1 = id;
    $this.y1e_1 = name;
    if (0 === (seen0 & 4))
      $this.z1e_1 = null;
    else
      $this.z1e_1 = versionOf;
    $this.a1f_1 = createdAt;
    $this.b1f_1 = updatedAt;
    if (0 === (seen0 & 32))
      $this.c1f_1 = null;
    else
      $this.c1f_1 = deletedAt;
    return $this;
  }
  function TemplateDto_init_$Create$(seen0, id, name, versionOf, createdAt, updatedAt, deletedAt, serializationConstructorMarker) {
    return TemplateDto_init_$Init$(seen0, id, name, versionOf, createdAt, updatedAt, deletedAt, serializationConstructorMarker, objectCreate(protoOf(TemplateDto)));
  }
  function TemplateDto() {
  }
  protoOf(TemplateDto).toString = function () {
    return 'TemplateDto(id=' + this.x1e_1 + ', name=' + this.y1e_1 + ', versionOf=' + this.z1e_1 + ', createdAt=' + this.a1f_1.toString() + ', updatedAt=' + this.b1f_1.toString() + ', deletedAt=' + toString_0(this.c1f_1) + ')';
  };
  protoOf(TemplateDto).hashCode = function () {
    var result = getStringHashCode(this.x1e_1);
    result = imul(result, 31) + getStringHashCode(this.y1e_1) | 0;
    result = imul(result, 31) + (this.z1e_1 == null ? 0 : getStringHashCode(this.z1e_1)) | 0;
    result = imul(result, 31) + this.a1f_1.hashCode() | 0;
    result = imul(result, 31) + this.b1f_1.hashCode() | 0;
    result = imul(result, 31) + (this.c1f_1 == null ? 0 : this.c1f_1.hashCode()) | 0;
    return result;
  };
  protoOf(TemplateDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof TemplateDto))
      return false;
    if (!(this.x1e_1 === other.x1e_1))
      return false;
    if (!(this.y1e_1 === other.y1e_1))
      return false;
    if (!(this.z1e_1 == other.z1e_1))
      return false;
    if (!equalsLong(this.a1f_1, other.a1f_1))
      return false;
    if (!equalsLong(this.b1f_1, other.b1f_1))
      return false;
    if (!equals(this.c1f_1, other.c1f_1))
      return false;
    return true;
  };
  function $serializer_9() {
    $serializer_instance_9 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.TemplateExerciseDto', this, 11);
    tmp0_serialDesc.zp('id', false);
    tmp0_serialDesc.zp('templateId', false);
    tmp0_serialDesc.zp('exerciseId', false);
    tmp0_serialDesc.zp('position', false);
    tmp0_serialDesc.zp('restSeconds', true);
    tmp0_serialDesc.zp('techniqueNote', true);
    tmp0_serialDesc.zp('targetRpeHalves', true);
    tmp0_serialDesc.zp('supersetGroup', true);
    tmp0_serialDesc.zp('createdAt', false);
    tmp0_serialDesc.zp('updatedAt', false);
    tmp0_serialDesc.zp('deletedAt', true);
    this.d1f_1 = tmp0_serialDesc;
  }
  protoOf($serializer_9).e1f = function (encoder, value) {
    var tmp0_desc = this.d1f_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    tmp1_output.fk(tmp0_desc, 0, value.f1f_1);
    tmp1_output.fk(tmp0_desc, 1, value.g1f_1);
    tmp1_output.fk(tmp0_desc, 2, value.h1f_1);
    tmp1_output.ak(tmp0_desc, 3, value.i1f_1);
    if (tmp1_output.nk(tmp0_desc, 4) ? true : !(value.j1f_1 == null)) {
      tmp1_output.jk(tmp0_desc, 4, IntSerializer_getInstance(), value.j1f_1);
    }
    if (tmp1_output.nk(tmp0_desc, 5) ? true : !(value.k1f_1 == null)) {
      tmp1_output.jk(tmp0_desc, 5, StringSerializer_getInstance(), value.k1f_1);
    }
    if (tmp1_output.nk(tmp0_desc, 6) ? true : !(value.l1f_1 == null)) {
      tmp1_output.jk(tmp0_desc, 6, IntSerializer_getInstance(), value.l1f_1);
    }
    if (tmp1_output.nk(tmp0_desc, 7) ? true : !(value.m1f_1 == null)) {
      tmp1_output.jk(tmp0_desc, 7, IntSerializer_getInstance(), value.m1f_1);
    }
    tmp1_output.bk(tmp0_desc, 8, value.n1f_1);
    tmp1_output.bk(tmp0_desc, 9, value.o1f_1);
    if (tmp1_output.nk(tmp0_desc, 10) ? true : !(value.p1f_1 == null)) {
      tmp1_output.jk(tmp0_desc, 10, LongSerializer_getInstance(), value.p1f_1);
    }
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_9).pf = function (encoder, value) {
    return this.e1f(encoder, value instanceof TemplateExerciseDto ? value : THROW_CCE());
  };
  protoOf($serializer_9).qf = function (decoder) {
    var tmp0_desc = this.d1f_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_local3 = 0;
    var tmp8_local4 = null;
    var tmp9_local5 = null;
    var tmp10_local6 = null;
    var tmp11_local7 = null;
    var tmp12_local8 = new Long(0, 0);
    var tmp13_local9 = new Long(0, 0);
    var tmp14_local10 = null;
    var tmp15_input = decoder.qi(tmp0_desc);
    if (tmp15_input.gj()) {
      tmp4_local0 = tmp15_input.aj(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp15_input.aj(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp15_input.aj(tmp0_desc, 2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp15_input.vi(tmp0_desc, 3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp15_input.ej(tmp0_desc, 4, IntSerializer_getInstance(), tmp8_local4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp15_input.ej(tmp0_desc, 5, StringSerializer_getInstance(), tmp9_local5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp15_input.ej(tmp0_desc, 6, IntSerializer_getInstance(), tmp10_local6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
      tmp11_local7 = tmp15_input.ej(tmp0_desc, 7, IntSerializer_getInstance(), tmp11_local7);
      tmp3_bitMask0 = tmp3_bitMask0 | 128;
      tmp12_local8 = tmp15_input.wi(tmp0_desc, 8);
      tmp3_bitMask0 = tmp3_bitMask0 | 256;
      tmp13_local9 = tmp15_input.wi(tmp0_desc, 9);
      tmp3_bitMask0 = tmp3_bitMask0 | 512;
      tmp14_local10 = tmp15_input.ej(tmp0_desc, 10, LongSerializer_getInstance(), tmp14_local10);
      tmp3_bitMask0 = tmp3_bitMask0 | 1024;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp15_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp15_input.aj(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp15_input.aj(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp15_input.aj(tmp0_desc, 2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp15_input.vi(tmp0_desc, 3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp15_input.ej(tmp0_desc, 4, IntSerializer_getInstance(), tmp8_local4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp15_input.ej(tmp0_desc, 5, StringSerializer_getInstance(), tmp9_local5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp15_input.ej(tmp0_desc, 6, IntSerializer_getInstance(), tmp10_local6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          case 7:
            tmp11_local7 = tmp15_input.ej(tmp0_desc, 7, IntSerializer_getInstance(), tmp11_local7);
            tmp3_bitMask0 = tmp3_bitMask0 | 128;
            break;
          case 8:
            tmp12_local8 = tmp15_input.wi(tmp0_desc, 8);
            tmp3_bitMask0 = tmp3_bitMask0 | 256;
            break;
          case 9:
            tmp13_local9 = tmp15_input.wi(tmp0_desc, 9);
            tmp3_bitMask0 = tmp3_bitMask0 | 512;
            break;
          case 10:
            tmp14_local10 = tmp15_input.ej(tmp0_desc, 10, LongSerializer_getInstance(), tmp14_local10);
            tmp3_bitMask0 = tmp3_bitMask0 | 1024;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp15_input.ri(tmp0_desc);
    return TemplateExerciseDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, tmp11_local7, tmp12_local8, tmp13_local9, tmp14_local10, null);
  };
  protoOf($serializer_9).of = function () {
    return this.d1f_1;
  };
  protoOf($serializer_9).oq = function () {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), StringSerializer_getInstance(), IntSerializer_getInstance(), get_nullable(IntSerializer_getInstance()), get_nullable(StringSerializer_getInstance()), get_nullable(IntSerializer_getInstance()), get_nullable(IntSerializer_getInstance()), LongSerializer_getInstance(), LongSerializer_getInstance(), get_nullable(LongSerializer_getInstance())];
  };
  var $serializer_instance_9;
  function $serializer_getInstance_9() {
    if ($serializer_instance_9 == null)
      new $serializer_9();
    return $serializer_instance_9;
  }
  function TemplateExerciseDto_init_$Init$(seen0, id, templateId, exerciseId, position, restSeconds, techniqueNote, targetRpeHalves, supersetGroup, createdAt, updatedAt, deletedAt, serializationConstructorMarker, $this) {
    if (!(783 === (783 & seen0))) {
      throwMissingFieldException(seen0, 783, $serializer_getInstance_9().d1f_1);
    }
    $this.f1f_1 = id;
    $this.g1f_1 = templateId;
    $this.h1f_1 = exerciseId;
    $this.i1f_1 = position;
    if (0 === (seen0 & 16))
      $this.j1f_1 = null;
    else
      $this.j1f_1 = restSeconds;
    if (0 === (seen0 & 32))
      $this.k1f_1 = null;
    else
      $this.k1f_1 = techniqueNote;
    if (0 === (seen0 & 64))
      $this.l1f_1 = null;
    else
      $this.l1f_1 = targetRpeHalves;
    if (0 === (seen0 & 128))
      $this.m1f_1 = null;
    else
      $this.m1f_1 = supersetGroup;
    $this.n1f_1 = createdAt;
    $this.o1f_1 = updatedAt;
    if (0 === (seen0 & 1024))
      $this.p1f_1 = null;
    else
      $this.p1f_1 = deletedAt;
    return $this;
  }
  function TemplateExerciseDto_init_$Create$(seen0, id, templateId, exerciseId, position, restSeconds, techniqueNote, targetRpeHalves, supersetGroup, createdAt, updatedAt, deletedAt, serializationConstructorMarker) {
    return TemplateExerciseDto_init_$Init$(seen0, id, templateId, exerciseId, position, restSeconds, techniqueNote, targetRpeHalves, supersetGroup, createdAt, updatedAt, deletedAt, serializationConstructorMarker, objectCreate(protoOf(TemplateExerciseDto)));
  }
  function TemplateExerciseDto() {
  }
  protoOf(TemplateExerciseDto).toString = function () {
    return 'TemplateExerciseDto(id=' + this.f1f_1 + ', templateId=' + this.g1f_1 + ', exerciseId=' + this.h1f_1 + ', position=' + this.i1f_1 + ', restSeconds=' + this.j1f_1 + ', techniqueNote=' + this.k1f_1 + ', targetRpeHalves=' + this.l1f_1 + ', supersetGroup=' + this.m1f_1 + ', createdAt=' + this.n1f_1.toString() + ', updatedAt=' + this.o1f_1.toString() + ', deletedAt=' + toString_0(this.p1f_1) + ')';
  };
  protoOf(TemplateExerciseDto).hashCode = function () {
    var result = getStringHashCode(this.f1f_1);
    result = imul(result, 31) + getStringHashCode(this.g1f_1) | 0;
    result = imul(result, 31) + getStringHashCode(this.h1f_1) | 0;
    result = imul(result, 31) + this.i1f_1 | 0;
    result = imul(result, 31) + (this.j1f_1 == null ? 0 : this.j1f_1) | 0;
    result = imul(result, 31) + (this.k1f_1 == null ? 0 : getStringHashCode(this.k1f_1)) | 0;
    result = imul(result, 31) + (this.l1f_1 == null ? 0 : this.l1f_1) | 0;
    result = imul(result, 31) + (this.m1f_1 == null ? 0 : this.m1f_1) | 0;
    result = imul(result, 31) + this.n1f_1.hashCode() | 0;
    result = imul(result, 31) + this.o1f_1.hashCode() | 0;
    result = imul(result, 31) + (this.p1f_1 == null ? 0 : this.p1f_1.hashCode()) | 0;
    return result;
  };
  protoOf(TemplateExerciseDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof TemplateExerciseDto))
      return false;
    if (!(this.f1f_1 === other.f1f_1))
      return false;
    if (!(this.g1f_1 === other.g1f_1))
      return false;
    if (!(this.h1f_1 === other.h1f_1))
      return false;
    if (!(this.i1f_1 === other.i1f_1))
      return false;
    if (!(this.j1f_1 == other.j1f_1))
      return false;
    if (!(this.k1f_1 == other.k1f_1))
      return false;
    if (!(this.l1f_1 == other.l1f_1))
      return false;
    if (!(this.m1f_1 == other.m1f_1))
      return false;
    if (!equalsLong(this.n1f_1, other.n1f_1))
      return false;
    if (!equalsLong(this.o1f_1, other.o1f_1))
      return false;
    if (!equals(this.p1f_1, other.p1f_1))
      return false;
    return true;
  };
  function TemplateSetDto$Companion$$childSerializers$_anonymous__so0lp() {
    return Companion_getInstance_11().b12();
  }
  function Companion_8() {
    Companion_instance_9 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.r1f_1 = [null, null, null, lazy(tmp_0, TemplateSetDto$Companion$$childSerializers$_anonymous__so0lp), null, null, null, null, null, null, null, null, null, null];
  }
  var Companion_instance_9;
  function Companion_getInstance_8() {
    if (Companion_instance_9 == null)
      new Companion_8();
    return Companion_instance_9;
  }
  function $serializer_10() {
    $serializer_instance_10 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.TemplateSetDto', this, 14);
    tmp0_serialDesc.zp('id', false);
    tmp0_serialDesc.zp('templateExerciseId', false);
    tmp0_serialDesc.zp('setIndex', false);
    tmp0_serialDesc.zp('role', false);
    tmp0_serialDesc.zp('targetWeightGrams', true);
    tmp0_serialDesc.zp('targetAssistanceGrams', true);
    tmp0_serialDesc.zp('targetRepsMin', true);
    tmp0_serialDesc.zp('targetRepsMax', true);
    tmp0_serialDesc.zp('targetRepsCurrent', true);
    tmp0_serialDesc.zp('dropValueGrams', true);
    tmp0_serialDesc.zp('note', true);
    tmp0_serialDesc.zp('createdAt', false);
    tmp0_serialDesc.zp('updatedAt', false);
    tmp0_serialDesc.zp('deletedAt', true);
    this.s1f_1 = tmp0_serialDesc;
  }
  protoOf($serializer_10).t1f = function (encoder, value) {
    var tmp0_desc = this.s1f_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    var tmp2_cached = Companion_getInstance_8().r1f_1;
    tmp1_output.fk(tmp0_desc, 0, value.u1f_1);
    tmp1_output.fk(tmp0_desc, 1, value.v1f_1);
    tmp1_output.ak(tmp0_desc, 2, value.w1f_1);
    tmp1_output.hk(tmp0_desc, 3, tmp2_cached[3].y1(), value.x1f_1);
    if (tmp1_output.nk(tmp0_desc, 4) ? true : !(value.y1f_1 == null)) {
      tmp1_output.jk(tmp0_desc, 4, LongSerializer_getInstance(), value.y1f_1);
    }
    if (tmp1_output.nk(tmp0_desc, 5) ? true : !(value.z1f_1 == null)) {
      tmp1_output.jk(tmp0_desc, 5, LongSerializer_getInstance(), value.z1f_1);
    }
    if (tmp1_output.nk(tmp0_desc, 6) ? true : !(value.a1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 6, IntSerializer_getInstance(), value.a1g_1);
    }
    if (tmp1_output.nk(tmp0_desc, 7) ? true : !(value.b1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 7, IntSerializer_getInstance(), value.b1g_1);
    }
    if (tmp1_output.nk(tmp0_desc, 8) ? true : !(value.c1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 8, IntSerializer_getInstance(), value.c1g_1);
    }
    if (tmp1_output.nk(tmp0_desc, 9) ? true : !(value.d1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 9, LongSerializer_getInstance(), value.d1g_1);
    }
    if (tmp1_output.nk(tmp0_desc, 10) ? true : !(value.e1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 10, StringSerializer_getInstance(), value.e1g_1);
    }
    tmp1_output.bk(tmp0_desc, 11, value.f1g_1);
    tmp1_output.bk(tmp0_desc, 12, value.g1g_1);
    if (tmp1_output.nk(tmp0_desc, 13) ? true : !(value.h1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 13, LongSerializer_getInstance(), value.h1g_1);
    }
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_10).pf = function (encoder, value) {
    return this.t1f(encoder, value instanceof TemplateSetDto ? value : THROW_CCE());
  };
  protoOf($serializer_10).qf = function (decoder) {
    var tmp0_desc = this.s1f_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = 0;
    var tmp7_local3 = null;
    var tmp8_local4 = null;
    var tmp9_local5 = null;
    var tmp10_local6 = null;
    var tmp11_local7 = null;
    var tmp12_local8 = null;
    var tmp13_local9 = null;
    var tmp14_local10 = null;
    var tmp15_local11 = new Long(0, 0);
    var tmp16_local12 = new Long(0, 0);
    var tmp17_local13 = null;
    var tmp18_input = decoder.qi(tmp0_desc);
    var tmp19_cached = Companion_getInstance_8().r1f_1;
    if (tmp18_input.gj()) {
      tmp4_local0 = tmp18_input.aj(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp18_input.aj(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp18_input.vi(tmp0_desc, 2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp18_input.cj(tmp0_desc, 3, tmp19_cached[3].y1(), tmp7_local3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp18_input.ej(tmp0_desc, 4, LongSerializer_getInstance(), tmp8_local4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp18_input.ej(tmp0_desc, 5, LongSerializer_getInstance(), tmp9_local5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp18_input.ej(tmp0_desc, 6, IntSerializer_getInstance(), tmp10_local6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
      tmp11_local7 = tmp18_input.ej(tmp0_desc, 7, IntSerializer_getInstance(), tmp11_local7);
      tmp3_bitMask0 = tmp3_bitMask0 | 128;
      tmp12_local8 = tmp18_input.ej(tmp0_desc, 8, IntSerializer_getInstance(), tmp12_local8);
      tmp3_bitMask0 = tmp3_bitMask0 | 256;
      tmp13_local9 = tmp18_input.ej(tmp0_desc, 9, LongSerializer_getInstance(), tmp13_local9);
      tmp3_bitMask0 = tmp3_bitMask0 | 512;
      tmp14_local10 = tmp18_input.ej(tmp0_desc, 10, StringSerializer_getInstance(), tmp14_local10);
      tmp3_bitMask0 = tmp3_bitMask0 | 1024;
      tmp15_local11 = tmp18_input.wi(tmp0_desc, 11);
      tmp3_bitMask0 = tmp3_bitMask0 | 2048;
      tmp16_local12 = tmp18_input.wi(tmp0_desc, 12);
      tmp3_bitMask0 = tmp3_bitMask0 | 4096;
      tmp17_local13 = tmp18_input.ej(tmp0_desc, 13, LongSerializer_getInstance(), tmp17_local13);
      tmp3_bitMask0 = tmp3_bitMask0 | 8192;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp18_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp18_input.aj(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp18_input.aj(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp18_input.vi(tmp0_desc, 2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp18_input.cj(tmp0_desc, 3, tmp19_cached[3].y1(), tmp7_local3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp18_input.ej(tmp0_desc, 4, LongSerializer_getInstance(), tmp8_local4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp18_input.ej(tmp0_desc, 5, LongSerializer_getInstance(), tmp9_local5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp18_input.ej(tmp0_desc, 6, IntSerializer_getInstance(), tmp10_local6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          case 7:
            tmp11_local7 = tmp18_input.ej(tmp0_desc, 7, IntSerializer_getInstance(), tmp11_local7);
            tmp3_bitMask0 = tmp3_bitMask0 | 128;
            break;
          case 8:
            tmp12_local8 = tmp18_input.ej(tmp0_desc, 8, IntSerializer_getInstance(), tmp12_local8);
            tmp3_bitMask0 = tmp3_bitMask0 | 256;
            break;
          case 9:
            tmp13_local9 = tmp18_input.ej(tmp0_desc, 9, LongSerializer_getInstance(), tmp13_local9);
            tmp3_bitMask0 = tmp3_bitMask0 | 512;
            break;
          case 10:
            tmp14_local10 = tmp18_input.ej(tmp0_desc, 10, StringSerializer_getInstance(), tmp14_local10);
            tmp3_bitMask0 = tmp3_bitMask0 | 1024;
            break;
          case 11:
            tmp15_local11 = tmp18_input.wi(tmp0_desc, 11);
            tmp3_bitMask0 = tmp3_bitMask0 | 2048;
            break;
          case 12:
            tmp16_local12 = tmp18_input.wi(tmp0_desc, 12);
            tmp3_bitMask0 = tmp3_bitMask0 | 4096;
            break;
          case 13:
            tmp17_local13 = tmp18_input.ej(tmp0_desc, 13, LongSerializer_getInstance(), tmp17_local13);
            tmp3_bitMask0 = tmp3_bitMask0 | 8192;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp18_input.ri(tmp0_desc);
    return TemplateSetDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, tmp11_local7, tmp12_local8, tmp13_local9, tmp14_local10, tmp15_local11, tmp16_local12, tmp17_local13, null);
  };
  protoOf($serializer_10).of = function () {
    return this.s1f_1;
  };
  protoOf($serializer_10).oq = function () {
    var tmp0_cached = Companion_getInstance_8().r1f_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), IntSerializer_getInstance(), tmp0_cached[3].y1(), get_nullable(LongSerializer_getInstance()), get_nullable(LongSerializer_getInstance()), get_nullable(IntSerializer_getInstance()), get_nullable(IntSerializer_getInstance()), get_nullable(IntSerializer_getInstance()), get_nullable(LongSerializer_getInstance()), get_nullable(StringSerializer_getInstance()), LongSerializer_getInstance(), LongSerializer_getInstance(), get_nullable(LongSerializer_getInstance())];
  };
  var $serializer_instance_10;
  function $serializer_getInstance_10() {
    if ($serializer_instance_10 == null)
      new $serializer_10();
    return $serializer_instance_10;
  }
  function TemplateSetDto_init_$Init$(seen0, id, templateExerciseId, setIndex, role, targetWeightGrams, targetAssistanceGrams, targetRepsMin, targetRepsMax, targetRepsCurrent, dropValueGrams, note, createdAt, updatedAt, deletedAt, serializationConstructorMarker, $this) {
    if (!(6159 === (6159 & seen0))) {
      throwMissingFieldException(seen0, 6159, $serializer_getInstance_10().s1f_1);
    }
    $this.u1f_1 = id;
    $this.v1f_1 = templateExerciseId;
    $this.w1f_1 = setIndex;
    $this.x1f_1 = role;
    if (0 === (seen0 & 16))
      $this.y1f_1 = null;
    else
      $this.y1f_1 = targetWeightGrams;
    if (0 === (seen0 & 32))
      $this.z1f_1 = null;
    else
      $this.z1f_1 = targetAssistanceGrams;
    if (0 === (seen0 & 64))
      $this.a1g_1 = null;
    else
      $this.a1g_1 = targetRepsMin;
    if (0 === (seen0 & 128))
      $this.b1g_1 = null;
    else
      $this.b1g_1 = targetRepsMax;
    if (0 === (seen0 & 256))
      $this.c1g_1 = null;
    else
      $this.c1g_1 = targetRepsCurrent;
    if (0 === (seen0 & 512))
      $this.d1g_1 = null;
    else
      $this.d1g_1 = dropValueGrams;
    if (0 === (seen0 & 1024))
      $this.e1g_1 = null;
    else
      $this.e1g_1 = note;
    $this.f1g_1 = createdAt;
    $this.g1g_1 = updatedAt;
    if (0 === (seen0 & 8192))
      $this.h1g_1 = null;
    else
      $this.h1g_1 = deletedAt;
    return $this;
  }
  function TemplateSetDto_init_$Create$(seen0, id, templateExerciseId, setIndex, role, targetWeightGrams, targetAssistanceGrams, targetRepsMin, targetRepsMax, targetRepsCurrent, dropValueGrams, note, createdAt, updatedAt, deletedAt, serializationConstructorMarker) {
    return TemplateSetDto_init_$Init$(seen0, id, templateExerciseId, setIndex, role, targetWeightGrams, targetAssistanceGrams, targetRepsMin, targetRepsMax, targetRepsCurrent, dropValueGrams, note, createdAt, updatedAt, deletedAt, serializationConstructorMarker, objectCreate(protoOf(TemplateSetDto)));
  }
  function TemplateSetDto() {
  }
  protoOf(TemplateSetDto).toString = function () {
    return 'TemplateSetDto(id=' + this.u1f_1 + ', templateExerciseId=' + this.v1f_1 + ', setIndex=' + this.w1f_1 + ', role=' + this.x1f_1.toString() + ', targetWeightGrams=' + toString_0(this.y1f_1) + ', targetAssistanceGrams=' + toString_0(this.z1f_1) + ', targetRepsMin=' + this.a1g_1 + ', targetRepsMax=' + this.b1g_1 + ', targetRepsCurrent=' + this.c1g_1 + ', dropValueGrams=' + toString_0(this.d1g_1) + ', note=' + this.e1g_1 + ', createdAt=' + this.f1g_1.toString() + ', updatedAt=' + this.g1g_1.toString() + ', deletedAt=' + toString_0(this.h1g_1) + ')';
  };
  protoOf(TemplateSetDto).hashCode = function () {
    var result = getStringHashCode(this.u1f_1);
    result = imul(result, 31) + getStringHashCode(this.v1f_1) | 0;
    result = imul(result, 31) + this.w1f_1 | 0;
    result = imul(result, 31) + this.x1f_1.hashCode() | 0;
    result = imul(result, 31) + (this.y1f_1 == null ? 0 : this.y1f_1.hashCode()) | 0;
    result = imul(result, 31) + (this.z1f_1 == null ? 0 : this.z1f_1.hashCode()) | 0;
    result = imul(result, 31) + (this.a1g_1 == null ? 0 : this.a1g_1) | 0;
    result = imul(result, 31) + (this.b1g_1 == null ? 0 : this.b1g_1) | 0;
    result = imul(result, 31) + (this.c1g_1 == null ? 0 : this.c1g_1) | 0;
    result = imul(result, 31) + (this.d1g_1 == null ? 0 : this.d1g_1.hashCode()) | 0;
    result = imul(result, 31) + (this.e1g_1 == null ? 0 : getStringHashCode(this.e1g_1)) | 0;
    result = imul(result, 31) + this.f1g_1.hashCode() | 0;
    result = imul(result, 31) + this.g1g_1.hashCode() | 0;
    result = imul(result, 31) + (this.h1g_1 == null ? 0 : this.h1g_1.hashCode()) | 0;
    return result;
  };
  protoOf(TemplateSetDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof TemplateSetDto))
      return false;
    if (!(this.u1f_1 === other.u1f_1))
      return false;
    if (!(this.v1f_1 === other.v1f_1))
      return false;
    if (!(this.w1f_1 === other.w1f_1))
      return false;
    if (!this.x1f_1.equals(other.x1f_1))
      return false;
    if (!equals(this.y1f_1, other.y1f_1))
      return false;
    if (!equals(this.z1f_1, other.z1f_1))
      return false;
    if (!(this.a1g_1 == other.a1g_1))
      return false;
    if (!(this.b1g_1 == other.b1g_1))
      return false;
    if (!(this.c1g_1 == other.c1g_1))
      return false;
    if (!equals(this.d1g_1, other.d1g_1))
      return false;
    if (!(this.e1g_1 == other.e1g_1))
      return false;
    if (!equalsLong(this.f1g_1, other.f1g_1))
      return false;
    if (!equalsLong(this.g1g_1, other.g1g_1))
      return false;
    if (!equals(this.h1g_1, other.h1g_1))
      return false;
    return true;
  };
  function ExerciseDto$Companion$$childSerializers$_anonymous__falojn() {
    return new ArrayListSerializer(StringSerializer_getInstance());
  }
  function Companion_9() {
    Companion_instance_10 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.i1g_1 = [null, null, null, lazy(tmp_0, ExerciseDto$Companion$$childSerializers$_anonymous__falojn), null, null, null, null, null, null, null, null, null, null, null, null, null];
  }
  var Companion_instance_10;
  function Companion_getInstance_9() {
    if (Companion_instance_10 == null)
      new Companion_9();
    return Companion_instance_10;
  }
  function $serializer_11() {
    $serializer_instance_11 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.ExerciseDto', this, 17);
    tmp0_serialDesc.zp('id', false);
    tmp0_serialDesc.zp('name', false);
    tmp0_serialDesc.zp('primaryMuscle', false);
    tmp0_serialDesc.zp('secondaryMuscles', false);
    tmp0_serialDesc.zp('equipment', false);
    tmp0_serialDesc.zp('movementPattern', false);
    tmp0_serialDesc.zp('createdAt', false);
    tmp0_serialDesc.zp('updatedAt', false);
    tmp0_serialDesc.zp('deletedAt', true);
    tmp0_serialDesc.zp('restSeconds', true);
    tmp0_serialDesc.zp('techniqueNote', true);
    tmp0_serialDesc.zp('weightUnit', true);
    tmp0_serialDesc.zp('stepGrams', true);
    tmp0_serialDesc.zp('parentId', true);
    tmp0_serialDesc.zp('rowKind', true);
    tmp0_serialDesc.zp('isBodyweight', true);
    tmp0_serialDesc.zp('isTimed', true);
    this.j1g_1 = tmp0_serialDesc;
  }
  protoOf($serializer_11).k1g = function (encoder, value) {
    var tmp0_desc = this.j1g_1;
    var tmp1_output = encoder.qi(tmp0_desc);
    var tmp2_cached = Companion_getInstance_9().i1g_1;
    tmp1_output.fk(tmp0_desc, 0, value.l1g_1);
    tmp1_output.fk(tmp0_desc, 1, value.m1g_1);
    tmp1_output.jk(tmp0_desc, 2, StringSerializer_getInstance(), value.n1g_1);
    tmp1_output.hk(tmp0_desc, 3, tmp2_cached[3].y1(), value.o1g_1);
    tmp1_output.jk(tmp0_desc, 4, StringSerializer_getInstance(), value.p1g_1);
    tmp1_output.jk(tmp0_desc, 5, StringSerializer_getInstance(), value.q1g_1);
    tmp1_output.bk(tmp0_desc, 6, value.r1g_1);
    tmp1_output.bk(tmp0_desc, 7, value.s1g_1);
    if (tmp1_output.nk(tmp0_desc, 8) ? true : !(value.t1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 8, LongSerializer_getInstance(), value.t1g_1);
    }
    if (tmp1_output.nk(tmp0_desc, 9) ? true : !(value.u1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 9, IntSerializer_getInstance(), value.u1g_1);
    }
    if (tmp1_output.nk(tmp0_desc, 10) ? true : !(value.v1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 10, StringSerializer_getInstance(), value.v1g_1);
    }
    if (tmp1_output.nk(tmp0_desc, 11) ? true : !(value.w1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 11, StringSerializer_getInstance(), value.w1g_1);
    }
    if (tmp1_output.nk(tmp0_desc, 12) ? true : !(value.x1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 12, LongSerializer_getInstance(), value.x1g_1);
    }
    if (tmp1_output.nk(tmp0_desc, 13) ? true : !(value.y1g_1 == null)) {
      tmp1_output.jk(tmp0_desc, 13, StringSerializer_getInstance(), value.y1g_1);
    }
    if (tmp1_output.nk(tmp0_desc, 14) ? true : !(value.z1g_1 === 'MOVEMENT')) {
      tmp1_output.fk(tmp0_desc, 14, value.z1g_1);
    }
    if (tmp1_output.nk(tmp0_desc, 15) ? true : !(value.a1h_1 === false)) {
      tmp1_output.xj(tmp0_desc, 15, value.a1h_1);
    }
    if (tmp1_output.nk(tmp0_desc, 16) ? true : !(value.b1h_1 === false)) {
      tmp1_output.xj(tmp0_desc, 16, value.b1h_1);
    }
    tmp1_output.ri(tmp0_desc);
  };
  protoOf($serializer_11).pf = function (encoder, value) {
    return this.k1g(encoder, value instanceof ExerciseDto ? value : THROW_CCE());
  };
  protoOf($serializer_11).qf = function (decoder) {
    var tmp0_desc = this.j1g_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_local3 = null;
    var tmp8_local4 = null;
    var tmp9_local5 = null;
    var tmp10_local6 = new Long(0, 0);
    var tmp11_local7 = new Long(0, 0);
    var tmp12_local8 = null;
    var tmp13_local9 = null;
    var tmp14_local10 = null;
    var tmp15_local11 = null;
    var tmp16_local12 = null;
    var tmp17_local13 = null;
    var tmp18_local14 = null;
    var tmp19_local15 = false;
    var tmp20_local16 = false;
    var tmp21_input = decoder.qi(tmp0_desc);
    var tmp22_cached = Companion_getInstance_9().i1g_1;
    if (tmp21_input.gj()) {
      tmp4_local0 = tmp21_input.aj(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp21_input.aj(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp21_input.ej(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp21_input.cj(tmp0_desc, 3, tmp22_cached[3].y1(), tmp7_local3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp21_input.ej(tmp0_desc, 4, StringSerializer_getInstance(), tmp8_local4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp21_input.ej(tmp0_desc, 5, StringSerializer_getInstance(), tmp9_local5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp21_input.wi(tmp0_desc, 6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
      tmp11_local7 = tmp21_input.wi(tmp0_desc, 7);
      tmp3_bitMask0 = tmp3_bitMask0 | 128;
      tmp12_local8 = tmp21_input.ej(tmp0_desc, 8, LongSerializer_getInstance(), tmp12_local8);
      tmp3_bitMask0 = tmp3_bitMask0 | 256;
      tmp13_local9 = tmp21_input.ej(tmp0_desc, 9, IntSerializer_getInstance(), tmp13_local9);
      tmp3_bitMask0 = tmp3_bitMask0 | 512;
      tmp14_local10 = tmp21_input.ej(tmp0_desc, 10, StringSerializer_getInstance(), tmp14_local10);
      tmp3_bitMask0 = tmp3_bitMask0 | 1024;
      tmp15_local11 = tmp21_input.ej(tmp0_desc, 11, StringSerializer_getInstance(), tmp15_local11);
      tmp3_bitMask0 = tmp3_bitMask0 | 2048;
      tmp16_local12 = tmp21_input.ej(tmp0_desc, 12, LongSerializer_getInstance(), tmp16_local12);
      tmp3_bitMask0 = tmp3_bitMask0 | 4096;
      tmp17_local13 = tmp21_input.ej(tmp0_desc, 13, StringSerializer_getInstance(), tmp17_local13);
      tmp3_bitMask0 = tmp3_bitMask0 | 8192;
      tmp18_local14 = tmp21_input.aj(tmp0_desc, 14);
      tmp3_bitMask0 = tmp3_bitMask0 | 16384;
      tmp19_local15 = tmp21_input.si(tmp0_desc, 15);
      tmp3_bitMask0 = tmp3_bitMask0 | 32768;
      tmp20_local16 = tmp21_input.si(tmp0_desc, 16);
      tmp3_bitMask0 = tmp3_bitMask0 | 65536;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp21_input.hj(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp21_input.aj(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp21_input.aj(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp21_input.ej(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp21_input.cj(tmp0_desc, 3, tmp22_cached[3].y1(), tmp7_local3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp21_input.ej(tmp0_desc, 4, StringSerializer_getInstance(), tmp8_local4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp21_input.ej(tmp0_desc, 5, StringSerializer_getInstance(), tmp9_local5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp21_input.wi(tmp0_desc, 6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          case 7:
            tmp11_local7 = tmp21_input.wi(tmp0_desc, 7);
            tmp3_bitMask0 = tmp3_bitMask0 | 128;
            break;
          case 8:
            tmp12_local8 = tmp21_input.ej(tmp0_desc, 8, LongSerializer_getInstance(), tmp12_local8);
            tmp3_bitMask0 = tmp3_bitMask0 | 256;
            break;
          case 9:
            tmp13_local9 = tmp21_input.ej(tmp0_desc, 9, IntSerializer_getInstance(), tmp13_local9);
            tmp3_bitMask0 = tmp3_bitMask0 | 512;
            break;
          case 10:
            tmp14_local10 = tmp21_input.ej(tmp0_desc, 10, StringSerializer_getInstance(), tmp14_local10);
            tmp3_bitMask0 = tmp3_bitMask0 | 1024;
            break;
          case 11:
            tmp15_local11 = tmp21_input.ej(tmp0_desc, 11, StringSerializer_getInstance(), tmp15_local11);
            tmp3_bitMask0 = tmp3_bitMask0 | 2048;
            break;
          case 12:
            tmp16_local12 = tmp21_input.ej(tmp0_desc, 12, LongSerializer_getInstance(), tmp16_local12);
            tmp3_bitMask0 = tmp3_bitMask0 | 4096;
            break;
          case 13:
            tmp17_local13 = tmp21_input.ej(tmp0_desc, 13, StringSerializer_getInstance(), tmp17_local13);
            tmp3_bitMask0 = tmp3_bitMask0 | 8192;
            break;
          case 14:
            tmp18_local14 = tmp21_input.aj(tmp0_desc, 14);
            tmp3_bitMask0 = tmp3_bitMask0 | 16384;
            break;
          case 15:
            tmp19_local15 = tmp21_input.si(tmp0_desc, 15);
            tmp3_bitMask0 = tmp3_bitMask0 | 32768;
            break;
          case 16:
            tmp20_local16 = tmp21_input.si(tmp0_desc, 16);
            tmp3_bitMask0 = tmp3_bitMask0 | 65536;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp21_input.ri(tmp0_desc);
    return ExerciseDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, tmp11_local7, tmp12_local8, tmp13_local9, tmp14_local10, tmp15_local11, tmp16_local12, tmp17_local13, tmp18_local14, tmp19_local15, tmp20_local16, null);
  };
  protoOf($serializer_11).of = function () {
    return this.j1g_1;
  };
  protoOf($serializer_11).oq = function () {
    var tmp0_cached = Companion_getInstance_9().i1g_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), get_nullable(StringSerializer_getInstance()), tmp0_cached[3].y1(), get_nullable(StringSerializer_getInstance()), get_nullable(StringSerializer_getInstance()), LongSerializer_getInstance(), LongSerializer_getInstance(), get_nullable(LongSerializer_getInstance()), get_nullable(IntSerializer_getInstance()), get_nullable(StringSerializer_getInstance()), get_nullable(StringSerializer_getInstance()), get_nullable(LongSerializer_getInstance()), get_nullable(StringSerializer_getInstance()), StringSerializer_getInstance(), BooleanSerializer_getInstance(), BooleanSerializer_getInstance()];
  };
  var $serializer_instance_11;
  function $serializer_getInstance_11() {
    if ($serializer_instance_11 == null)
      new $serializer_11();
    return $serializer_instance_11;
  }
  function ExerciseDto_init_$Init$(seen0, id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, createdAt, updatedAt, deletedAt, restSeconds, techniqueNote, weightUnit, stepGrams, parentId, rowKind, isBodyweight, isTimed, serializationConstructorMarker, $this) {
    if (!(255 === (255 & seen0))) {
      throwMissingFieldException(seen0, 255, $serializer_getInstance_11().j1g_1);
    }
    $this.l1g_1 = id;
    $this.m1g_1 = name;
    $this.n1g_1 = primaryMuscle;
    $this.o1g_1 = secondaryMuscles;
    $this.p1g_1 = equipment;
    $this.q1g_1 = movementPattern;
    $this.r1g_1 = createdAt;
    $this.s1g_1 = updatedAt;
    if (0 === (seen0 & 256))
      $this.t1g_1 = null;
    else
      $this.t1g_1 = deletedAt;
    if (0 === (seen0 & 512))
      $this.u1g_1 = null;
    else
      $this.u1g_1 = restSeconds;
    if (0 === (seen0 & 1024))
      $this.v1g_1 = null;
    else
      $this.v1g_1 = techniqueNote;
    if (0 === (seen0 & 2048))
      $this.w1g_1 = null;
    else
      $this.w1g_1 = weightUnit;
    if (0 === (seen0 & 4096))
      $this.x1g_1 = null;
    else
      $this.x1g_1 = stepGrams;
    if (0 === (seen0 & 8192))
      $this.y1g_1 = null;
    else
      $this.y1g_1 = parentId;
    if (0 === (seen0 & 16384))
      $this.z1g_1 = 'MOVEMENT';
    else
      $this.z1g_1 = rowKind;
    if (0 === (seen0 & 32768))
      $this.a1h_1 = false;
    else
      $this.a1h_1 = isBodyweight;
    if (0 === (seen0 & 65536))
      $this.b1h_1 = false;
    else
      $this.b1h_1 = isTimed;
    return $this;
  }
  function ExerciseDto_init_$Create$(seen0, id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, createdAt, updatedAt, deletedAt, restSeconds, techniqueNote, weightUnit, stepGrams, parentId, rowKind, isBodyweight, isTimed, serializationConstructorMarker) {
    return ExerciseDto_init_$Init$(seen0, id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, createdAt, updatedAt, deletedAt, restSeconds, techniqueNote, weightUnit, stepGrams, parentId, rowKind, isBodyweight, isTimed, serializationConstructorMarker, objectCreate(protoOf(ExerciseDto)));
  }
  function ExerciseDto() {
  }
  protoOf(ExerciseDto).toString = function () {
    return 'ExerciseDto(id=' + this.l1g_1 + ', name=' + this.m1g_1 + ', primaryMuscle=' + this.n1g_1 + ', secondaryMuscles=' + toString(this.o1g_1) + ', equipment=' + this.p1g_1 + ', movementPattern=' + this.q1g_1 + ', createdAt=' + this.r1g_1.toString() + ', updatedAt=' + this.s1g_1.toString() + ', deletedAt=' + toString_0(this.t1g_1) + ', restSeconds=' + this.u1g_1 + ', techniqueNote=' + this.v1g_1 + ', weightUnit=' + this.w1g_1 + ', stepGrams=' + toString_0(this.x1g_1) + ', parentId=' + this.y1g_1 + ', rowKind=' + this.z1g_1 + ', isBodyweight=' + this.a1h_1 + ', isTimed=' + this.b1h_1 + ')';
  };
  protoOf(ExerciseDto).hashCode = function () {
    var result = getStringHashCode(this.l1g_1);
    result = imul(result, 31) + getStringHashCode(this.m1g_1) | 0;
    result = imul(result, 31) + (this.n1g_1 == null ? 0 : getStringHashCode(this.n1g_1)) | 0;
    result = imul(result, 31) + hashCode(this.o1g_1) | 0;
    result = imul(result, 31) + (this.p1g_1 == null ? 0 : getStringHashCode(this.p1g_1)) | 0;
    result = imul(result, 31) + (this.q1g_1 == null ? 0 : getStringHashCode(this.q1g_1)) | 0;
    result = imul(result, 31) + this.r1g_1.hashCode() | 0;
    result = imul(result, 31) + this.s1g_1.hashCode() | 0;
    result = imul(result, 31) + (this.t1g_1 == null ? 0 : this.t1g_1.hashCode()) | 0;
    result = imul(result, 31) + (this.u1g_1 == null ? 0 : this.u1g_1) | 0;
    result = imul(result, 31) + (this.v1g_1 == null ? 0 : getStringHashCode(this.v1g_1)) | 0;
    result = imul(result, 31) + (this.w1g_1 == null ? 0 : getStringHashCode(this.w1g_1)) | 0;
    result = imul(result, 31) + (this.x1g_1 == null ? 0 : this.x1g_1.hashCode()) | 0;
    result = imul(result, 31) + (this.y1g_1 == null ? 0 : getStringHashCode(this.y1g_1)) | 0;
    result = imul(result, 31) + getStringHashCode(this.z1g_1) | 0;
    result = imul(result, 31) + getBooleanHashCode(this.a1h_1) | 0;
    result = imul(result, 31) + getBooleanHashCode(this.b1h_1) | 0;
    return result;
  };
  protoOf(ExerciseDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ExerciseDto))
      return false;
    if (!(this.l1g_1 === other.l1g_1))
      return false;
    if (!(this.m1g_1 === other.m1g_1))
      return false;
    if (!(this.n1g_1 == other.n1g_1))
      return false;
    if (!equals(this.o1g_1, other.o1g_1))
      return false;
    if (!(this.p1g_1 == other.p1g_1))
      return false;
    if (!(this.q1g_1 == other.q1g_1))
      return false;
    if (!equalsLong(this.r1g_1, other.r1g_1))
      return false;
    if (!equalsLong(this.s1g_1, other.s1g_1))
      return false;
    if (!equals(this.t1g_1, other.t1g_1))
      return false;
    if (!(this.u1g_1 == other.u1g_1))
      return false;
    if (!(this.v1g_1 == other.v1g_1))
      return false;
    if (!(this.w1g_1 == other.w1g_1))
      return false;
    if (!equals(this.x1g_1, other.x1g_1))
      return false;
    if (!(this.y1g_1 == other.y1g_1))
      return false;
    if (!(this.z1g_1 === other.z1g_1))
      return false;
    if (!(this.a1h_1 === other.a1h_1))
      return false;
    if (!(this.b1h_1 === other.b1h_1))
      return false;
    return true;
  };
  function _get_$cachedSerializer__te6jhj_0($this) {
    return $this.j1e_1.y1();
  }
  function Weekday$Companion$_anonymous__wjgbbt() {
    return createSimpleEnumSerializer('com.example.androidapp.shared.Weekday', values_0());
  }
  var static_init_called_1;
  function static_init_1() {
    if (static_init_called_1)
      return Unit_instance;
    static_init_called_1 = true;
    Weekday_MONDAY_instance = new Weekday('MONDAY', 0);
    Weekday_TUESDAY_instance = new Weekday('TUESDAY', 1);
    Weekday_WEDNESDAY_instance = new Weekday('WEDNESDAY', 2);
    Weekday_THURSDAY_instance = new Weekday('THURSDAY', 3);
    Weekday_FRIDAY_instance = new Weekday('FRIDAY', 4);
    Weekday_SATURDAY_instance = new Weekday('SATURDAY', 5);
    Weekday_SUNDAY_instance = new Weekday('SUNDAY', 6);
    if (Companion_instance_11 == null) {
      Companion_instance;
      new Companion_10();
    }
  }
  var Weekday_MONDAY_instance;
  var Weekday_TUESDAY_instance;
  var Weekday_WEDNESDAY_instance;
  var Weekday_THURSDAY_instance;
  var Weekday_FRIDAY_instance;
  var Weekday_SATURDAY_instance;
  var Weekday_SUNDAY_instance;
  function values_0() {
    static_init_1();
    return [Weekday_MONDAY_getInstance(), Weekday_TUESDAY_getInstance(), Weekday_WEDNESDAY_getInstance(), Weekday_THURSDAY_getInstance(), Weekday_FRIDAY_getInstance(), Weekday_SATURDAY_getInstance(), Weekday_SUNDAY_getInstance()];
  }
  function Companion_10() {
    Companion_instance_11 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp.j1e_1 = lazy(tmp_0, Weekday$Companion$_anonymous__wjgbbt);
  }
  protoOf(Companion_10).b12 = function () {
    return _get_$cachedSerializer__te6jhj_0(this);
  };
  protoOf(Companion_10).br = function (typeParamsSerializers) {
    return this.b12();
  };
  var Companion_instance_11;
  function Companion_getInstance_10() {
    static_init_1();
    return Companion_instance_11;
  }
  function Weekday(name, ordinal) {
    Enum.call(this, name, ordinal);
  }
  function _get_$cachedSerializer__te6jhj_1($this) {
    return $this.q1f_1.y1();
  }
  function ProgramSetRole$Companion$_anonymous__csnl4r() {
    return createSimpleEnumSerializer('com.example.androidapp.shared.ProgramSetRole', values_1());
  }
  var static_init_called_2;
  function static_init_2() {
    if (static_init_called_2)
      return Unit_instance;
    static_init_called_2 = true;
    ProgramSetRole_NORMAL_instance = new ProgramSetRole('NORMAL', 0);
    ProgramSetRole_WARMUP_instance = new ProgramSetRole('WARMUP', 1);
    ProgramSetRole_TOP_SET_instance = new ProgramSetRole('TOP_SET', 2);
    ProgramSetRole_DROP_instance = new ProgramSetRole('DROP', 3);
    ProgramSetRole_CLUSTER_instance = new ProgramSetRole('CLUSTER', 4);
    ProgramSetRole_FAILURE_instance = new ProgramSetRole('FAILURE', 5);
    if (Companion_instance_12 == null) {
      Companion_instance;
      new Companion_11();
    }
  }
  var ProgramSetRole_NORMAL_instance;
  var ProgramSetRole_WARMUP_instance;
  var ProgramSetRole_TOP_SET_instance;
  var ProgramSetRole_DROP_instance;
  var ProgramSetRole_CLUSTER_instance;
  var ProgramSetRole_FAILURE_instance;
  function values_1() {
    static_init_2();
    return [ProgramSetRole_NORMAL_getInstance(), ProgramSetRole_WARMUP_getInstance(), ProgramSetRole_TOP_SET_getInstance(), ProgramSetRole_DROP_getInstance(), ProgramSetRole_CLUSTER_getInstance(), ProgramSetRole_FAILURE_getInstance()];
  }
  function Companion_11() {
    Companion_instance_12 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp.q1f_1 = lazy(tmp_0, ProgramSetRole$Companion$_anonymous__csnl4r);
  }
  protoOf(Companion_11).b12 = function () {
    return _get_$cachedSerializer__te6jhj_1(this);
  };
  protoOf(Companion_11).br = function (typeParamsSerializers) {
    return this.b12();
  };
  var Companion_instance_12;
  function Companion_getInstance_11() {
    static_init_2();
    return Companion_instance_12;
  }
  function ProgramSetRole(name, ordinal) {
    Enum.call(this, name, ordinal);
  }
  protoOf(ProgramSetRole).e1h = function () {
    return this.equals(ProgramSetRole_DROP_getInstance()) || this.equals(ProgramSetRole_CLUSTER_getInstance());
  };
  function ProgramKind_WEEKLY_getInstance() {
    static_init_0();
    return ProgramKind_WEEKLY_instance;
  }
  function ProgramKind_SEQUENCE_getInstance() {
    static_init_0();
    return ProgramKind_SEQUENCE_instance;
  }
  function Weekday_MONDAY_getInstance() {
    static_init_1();
    return Weekday_MONDAY_instance;
  }
  function Weekday_TUESDAY_getInstance() {
    static_init_1();
    return Weekday_TUESDAY_instance;
  }
  function Weekday_WEDNESDAY_getInstance() {
    static_init_1();
    return Weekday_WEDNESDAY_instance;
  }
  function Weekday_THURSDAY_getInstance() {
    static_init_1();
    return Weekday_THURSDAY_instance;
  }
  function Weekday_FRIDAY_getInstance() {
    static_init_1();
    return Weekday_FRIDAY_instance;
  }
  function Weekday_SATURDAY_getInstance() {
    static_init_1();
    return Weekday_SATURDAY_instance;
  }
  function Weekday_SUNDAY_getInstance() {
    static_init_1();
    return Weekday_SUNDAY_instance;
  }
  function ProgramSetRole_NORMAL_getInstance() {
    static_init_2();
    return ProgramSetRole_NORMAL_instance;
  }
  function ProgramSetRole_WARMUP_getInstance() {
    static_init_2();
    return ProgramSetRole_WARMUP_instance;
  }
  function ProgramSetRole_TOP_SET_getInstance() {
    static_init_2();
    return ProgramSetRole_TOP_SET_instance;
  }
  function ProgramSetRole_DROP_getInstance() {
    static_init_2();
    return ProgramSetRole_DROP_instance;
  }
  function ProgramSetRole_CLUSTER_getInstance() {
    static_init_2();
    return ProgramSetRole_CLUSTER_instance;
  }
  function ProgramSetRole_FAILURE_getInstance() {
    static_init_2();
    return ProgramSetRole_FAILURE_instance;
  }
  var static_init_called_3;
  function static_init_3() {
    if (static_init_called_3)
      return Unit_instance;
    static_init_called_3 = true;
    ChangeKind_PROGRAM_RENAMED_instance = new ChangeKind('PROGRAM_RENAMED', 0);
    ChangeKind_PROGRAM_KIND_instance = new ChangeKind('PROGRAM_KIND', 1);
    ChangeKind_PROGRAM_START_instance = new ChangeKind('PROGRAM_START', 2);
    ChangeKind_SLOT_ADDED_instance = new ChangeKind('SLOT_ADDED', 3);
    ChangeKind_SLOT_UPDATED_instance = new ChangeKind('SLOT_UPDATED', 4);
    ChangeKind_SLOT_REMOVED_instance = new ChangeKind('SLOT_REMOVED', 5);
    ChangeKind_TEMPLATE_ADDED_instance = new ChangeKind('TEMPLATE_ADDED', 6);
    ChangeKind_TEMPLATE_UPDATED_instance = new ChangeKind('TEMPLATE_UPDATED', 7);
    ChangeKind_TEMPLATE_EXERCISE_ADDED_instance = new ChangeKind('TEMPLATE_EXERCISE_ADDED', 8);
    ChangeKind_TEMPLATE_EXERCISE_UPDATED_instance = new ChangeKind('TEMPLATE_EXERCISE_UPDATED', 9);
    ChangeKind_TEMPLATE_EXERCISE_REMOVED_instance = new ChangeKind('TEMPLATE_EXERCISE_REMOVED', 10);
    ChangeKind_SET_ADDED_instance = new ChangeKind('SET_ADDED', 11);
    ChangeKind_SET_UPDATED_instance = new ChangeKind('SET_UPDATED', 12);
    ChangeKind_SET_REMOVED_instance = new ChangeKind('SET_REMOVED', 13);
    ChangeKind_EXERCISE_ADDED_instance = new ChangeKind('EXERCISE_ADDED', 14);
  }
  var ChangeKind_PROGRAM_RENAMED_instance;
  var ChangeKind_PROGRAM_KIND_instance;
  var ChangeKind_PROGRAM_START_instance;
  var ChangeKind_SLOT_ADDED_instance;
  var ChangeKind_SLOT_UPDATED_instance;
  var ChangeKind_SLOT_REMOVED_instance;
  var ChangeKind_TEMPLATE_ADDED_instance;
  var ChangeKind_TEMPLATE_UPDATED_instance;
  var ChangeKind_TEMPLATE_EXERCISE_ADDED_instance;
  var ChangeKind_TEMPLATE_EXERCISE_UPDATED_instance;
  var ChangeKind_TEMPLATE_EXERCISE_REMOVED_instance;
  var ChangeKind_SET_ADDED_instance;
  var ChangeKind_SET_UPDATED_instance;
  var ChangeKind_SET_REMOVED_instance;
  var ChangeKind_EXERCISE_ADDED_instance;
  function ChangeKind(name, ordinal) {
    Enum.call(this, name, ordinal);
  }
  function Change(kind, id, name, detail) {
    name = name === VOID ? null : name;
    detail = detail === VOID ? null : detail;
    this.y1b_1 = kind;
    this.z1b_1 = id;
    this.a1c_1 = name;
    this.b1c_1 = detail;
  }
  protoOf(Change).toString = function () {
    return 'Change(kind=' + this.y1b_1.toString() + ', id=' + this.z1b_1 + ', name=' + this.a1c_1 + ', detail=' + this.b1c_1 + ')';
  };
  protoOf(Change).hashCode = function () {
    var result = this.y1b_1.hashCode();
    result = imul(result, 31) + getStringHashCode(this.z1b_1) | 0;
    result = imul(result, 31) + (this.a1c_1 == null ? 0 : getStringHashCode(this.a1c_1)) | 0;
    result = imul(result, 31) + (this.b1c_1 == null ? 0 : getStringHashCode(this.b1c_1)) | 0;
    return result;
  };
  protoOf(Change).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof Change))
      return false;
    if (!this.y1b_1.equals(other.y1b_1))
      return false;
    if (!(this.z1b_1 === other.z1b_1))
      return false;
    if (!(this.a1c_1 == other.a1c_1))
      return false;
    if (!(this.b1c_1 == other.b1c_1))
      return false;
    return true;
  };
  var static_init_called_4;
  function static_init_4() {
    if (static_init_called_4)
      return Unit_instance;
    static_init_called_4 = true;
    WarningCode_NEW_MOVEMENT_instance = new WarningCode('NEW_MOVEMENT', 0);
    WarningCode_WORKOUT_KEPT_instance = new WarningCode('WORKOUT_KEPT', 1);
  }
  var WarningCode_NEW_MOVEMENT_instance;
  var WarningCode_WORKOUT_KEPT_instance;
  function WarningCode(name, ordinal) {
    Enum.call(this, name, ordinal);
  }
  function PlanWarning(code, id, name) {
    id = id === VOID ? null : id;
    name = name === VOID ? null : name;
    this.c1c_1 = code;
    this.d1c_1 = id;
    this.e1c_1 = name;
  }
  protoOf(PlanWarning).toString = function () {
    return 'PlanWarning(code=' + this.c1c_1.toString() + ', id=' + this.d1c_1 + ', name=' + this.e1c_1 + ')';
  };
  protoOf(PlanWarning).hashCode = function () {
    var result = this.c1c_1.hashCode();
    result = imul(result, 31) + (this.d1c_1 == null ? 0 : getStringHashCode(this.d1c_1)) | 0;
    result = imul(result, 31) + (this.e1c_1 == null ? 0 : getStringHashCode(this.e1c_1)) | 0;
    return result;
  };
  protoOf(PlanWarning).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof PlanWarning))
      return false;
    if (!this.c1c_1.equals(other.c1c_1))
      return false;
    if (!(this.d1c_1 == other.d1c_1))
      return false;
    if (!(this.e1c_1 == other.e1c_1))
      return false;
    return true;
  };
  function Ready(changes, warnings) {
    this.w1b_1 = changes;
    this.x1b_1 = warnings;
  }
  protoOf(Ready).toString = function () {
    return 'Ready(changes=' + toString(this.w1b_1) + ', warnings=' + toString(this.x1b_1) + ')';
  };
  protoOf(Ready).hashCode = function () {
    var result = hashCode(this.w1b_1);
    result = imul(result, 31) + hashCode(this.x1b_1) | 0;
    return result;
  };
  protoOf(Ready).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof Ready))
      return false;
    if (!equals(this.w1b_1, other.w1b_1))
      return false;
    if (!equals(this.x1b_1, other.x1b_1))
      return false;
    return true;
  };
  function Refused(problems) {
    this.f1c_1 = problems;
  }
  protoOf(Refused).toString = function () {
    return 'Refused(problems=' + toString(this.f1c_1) + ')';
  };
  protoOf(Refused).hashCode = function () {
    return hashCode(this.f1c_1);
  };
  protoOf(Refused).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof Refused))
      return false;
    if (!equals(this.f1c_1, other.f1c_1))
      return false;
    return true;
  };
  function plan(snapshot, edit, deviceNewestWrite) {
    deviceNewestWrite = deviceNewestWrite === VOID ? null : deviceNewestWrite;
    var tmp0_safe_receiver = refusalProblems(snapshot, edit, deviceNewestWrite);
    if (tmp0_safe_receiver == null)
      null;
    else {
      // Inline function 'kotlin.let' call
      return new Refused(tmp0_safe_receiver);
    }
    // Inline function 'kotlin.collections.mutableListOf' call
    var warnings = ArrayList_init_$Create$_0();
    // Inline function 'kotlin.collections.buildList' call
    // Inline function 'kotlin.collections.buildListInternal' call
    // Inline function 'kotlin.apply' call
    var this_0 = ArrayList_init_$Create$_0();
    this_0.c1(planProgram(snapshot, edit));
    this_0.c1(planSlots(snapshot, edit, warnings));
    this_0.c1(planTemplates(snapshot, edit));
    this_0.c1(planTemplateExercises(snapshot, edit));
    this_0.c1(planSets(snapshot, edit));
    this_0.c1(planExercises(snapshot, edit, warnings));
    var changes = this_0.f4();
    return new Ready(changes, warnings);
  }
  function refusalProblems(snapshot, edit, deviceNewestWrite) {
    var marker = edit.s1d_1;
    var shape = marker == null ? new ProgramProblem(ProblemCode_NO_EDIT_MARKER_getInstance(), 'edit') : !(edit.m1d_1.a1e_1 === snapshot.m1d_1.a1e_1) ? new ProgramProblem(ProblemCode_PROGRAM_MISMATCH_getInstance(), 'program.id', edit.m1d_1.a1e_1) : !(snapshot.m1d_1.i1e_1 == null) ? new ProgramProblem(ProblemCode_PROGRAM_MISSING_getInstance(), 'program.id', edit.m1d_1.a1e_1) : !(edit.m1d_1.i1e_1 == null) ? new ProgramProblem(ProblemCode_MALFORMED_getInstance(), 'program.deletedAt') : null;
    var invalid = validate(edit);
    var tmp;
    if (marker == null) {
      tmp = null;
    } else {
      // Inline function 'kotlin.let' call
      tmp = staleProblem(snapshot, marker, deviceNewestWrite);
    }
    var stale = tmp;
    var tmp_0;
    if (shape == null) {
      tmp_0 = null;
    } else {
      // Inline function 'kotlin.let' call
      tmp_0 = listOf(shape);
    }
    var tmp2_elvis_lhs = tmp_0;
    var tmp_1;
    if (tmp2_elvis_lhs == null) {
      // Inline function 'kotlin.takeIf' call
      var tmp_2;
      // Inline function 'kotlin.collections.isNotEmpty' call
      if (!invalid.p()) {
        tmp_2 = invalid;
      } else {
        tmp_2 = null;
      }
      tmp_1 = tmp_2;
    } else {
      tmp_1 = tmp2_elvis_lhs;
    }
    var tmp3_elvis_lhs = tmp_1;
    var tmp_3;
    if (tmp3_elvis_lhs == null) {
      var tmp_4;
      if (stale == null) {
        tmp_4 = null;
      } else {
        // Inline function 'kotlin.let' call
        tmp_4 = listOf(stale);
      }
      tmp_3 = tmp_4;
    } else {
      tmp_3 = tmp3_elvis_lhs;
    }
    return tmp_3;
  }
  function staleProblem(snapshot, marker, deviceNewestWrite) {
    var fromDocument = latestWrite(snapshot);
    var tmp;
    if (deviceNewestWrite == null) {
      tmp = fromDocument;
    } else {
      // Inline function 'kotlin.comparisons.maxOf' call
      tmp = compare(fromDocument, deviceNewestWrite) >= 0 ? fromDocument : deviceNewestWrite;
    }
    var newestWrite = tmp;
    var tmp_0;
    if (compare(newestWrite, marker.v1d_1) > 0) {
      tmp_0 = new ProgramProblem(ProblemCode_STALE_BASE_getInstance(), 'edit.baseExportedAt', newestWrite.toString());
    } else {
      tmp_0 = null;
    }
    return tmp_0;
  }
  function planProgram(snapshot, edit) {
    // Inline function 'kotlin.collections.mutableListOf' call
    var changes = ArrayList_init_$Create$_0();
    if (!(snapshot.m1d_1.b1e_1 === edit.m1d_1.b1e_1)) {
      // Inline function 'kotlin.collections.plusAssign' call
      var element = new Change(ChangeKind_PROGRAM_RENAMED_getInstance(), edit.m1d_1.a1e_1, edit.m1d_1.b1e_1);
      changes.w(element);
    }
    if (!snapshot.m1d_1.e1e_1.equals(edit.m1d_1.e1e_1)) {
      // Inline function 'kotlin.collections.plusAssign' call
      var element_0 = new Change(ChangeKind_PROGRAM_KIND_getInstance(), edit.m1d_1.a1e_1, edit.m1d_1.b1e_1, edit.m1d_1.e1e_1.j2_1);
      changes.w(element_0);
    }
    if (!equals(snapshot.m1d_1.f1e_1, edit.m1d_1.f1e_1)) {
      var tmp = ChangeKind_PROGRAM_START_getInstance();
      var tmp0_safe_receiver = edit.m1d_1.f1e_1;
      // Inline function 'kotlin.collections.plusAssign' call
      var element_1 = new Change(tmp, edit.m1d_1.a1e_1, edit.m1d_1.b1e_1, tmp0_safe_receiver == null ? null : tmp0_safe_receiver.toString());
      changes.w(element_1);
    }
    return changes;
  }
  function planSlots(snapshot, edit, warnings) {
    // Inline function 'kotlin.collections.mutableListOf' call
    var changes = ArrayList_init_$Create$_0();
    // Inline function 'kotlin.collections.associateBy' call
    var this_0 = snapshot.n1d_1;
    var capacity = coerceAtLeast(mapCapacity(collectionSizeOrDefault(this_0, 10)), 16);
    // Inline function 'kotlin.collections.associateByTo' call
    var destination = LinkedHashMap_init_$Create$(capacity);
    var _iterator__ex2g4s = this_0.r();
    while (_iterator__ex2g4s.s()) {
      var element = _iterator__ex2g4s.t();
      var tmp$ret$3 = element.n1e_1;
      destination.h2(tmp$ret$3, element);
    }
    var snapshotSlots = destination;
    // Inline function 'kotlin.collections.map' call
    var this_1 = edit.n1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(this_1, 10));
    var _iterator__ex2g4s_0 = this_1.r();
    while (_iterator__ex2g4s_0.s()) {
      var item = _iterator__ex2g4s_0.t();
      var tmp$ret$6 = item.n1e_1;
      destination_0.w(tmp$ret$6);
    }
    var editSlotIds = toSet(destination_0);
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_1 = edit.n1d_1.r();
    while (_iterator__ex2g4s_1.s()) {
      var element_0 = _iterator__ex2g4s_1.t();
      var old = snapshotSlots.b2(element_0.n1e_1);
      var tmp0 = edit.o1d_1;
      var tmp$ret$9;
      $l$block: {
        // Inline function 'kotlin.collections.firstOrNull' call
        var _iterator__ex2g4s_2 = tmp0.r();
        while (_iterator__ex2g4s_2.s()) {
          var element_1 = _iterator__ex2g4s_2.t();
          if (element_1.x1e_1 === element_0.p1e_1) {
            tmp$ret$9 = element_1;
            break $l$block;
          }
        }
        tmp$ret$9 = null;
      }
      var tmp0_safe_receiver = tmp$ret$9;
      var name = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.y1e_1;
      if (old == null) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element_2 = new Change(ChangeKind_SLOT_ADDED_getInstance(), element_0.n1e_1, name);
        changes.w(element_2);
      } else if (!sameSlot(old, element_0)) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element_3 = new Change(ChangeKind_SLOT_UPDATED_getInstance(), element_0.n1e_1, name);
        changes.w(element_3);
      }
    }
    // Inline function 'kotlin.collections.filter' call
    var tmp0_0 = snapshot.n1d_1;
    // Inline function 'kotlin.collections.filterTo' call
    var destination_1 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_3 = tmp0_0.r();
    while (_iterator__ex2g4s_3.s()) {
      var element_4 = _iterator__ex2g4s_3.t();
      if (!editSlotIds.v1(element_4.n1e_1)) {
        destination_1.w(element_4);
      }
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_4 = destination_1.r();
    while (_iterator__ex2g4s_4.s()) {
      var element_5 = _iterator__ex2g4s_4.t();
      var tmp0_1 = snapshot.o1d_1;
      var tmp$ret$18;
      $l$block_0: {
        // Inline function 'kotlin.collections.firstOrNull' call
        var _iterator__ex2g4s_5 = tmp0_1.r();
        while (_iterator__ex2g4s_5.s()) {
          var element_6 = _iterator__ex2g4s_5.t();
          if (element_6.x1e_1 === element_5.p1e_1) {
            tmp$ret$18 = element_6;
            break $l$block_0;
          }
        }
        tmp$ret$18 = null;
      }
      var tmp0_safe_receiver_0 = tmp$ret$18;
      var name_0 = tmp0_safe_receiver_0 == null ? null : tmp0_safe_receiver_0.y1e_1;
      // Inline function 'kotlin.collections.plusAssign' call
      var element_7 = new Change(ChangeKind_SLOT_REMOVED_getInstance(), element_5.n1e_1, name_0);
      changes.w(element_7);
    }
    // Inline function 'kotlin.collections.plusAssign' call
    var elements = keptWorkoutWarnings(snapshot, edit, editSlotIds);
    addAll(warnings, elements);
    return changes;
  }
  function keptWorkoutWarnings(snapshot, edit, editSlotIds) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = edit.n1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var tmp$ret$2 = item.p1e_1;
      destination.w(tmp$ret$2);
    }
    var keptTemplateIds = toSet(destination);
    // Inline function 'kotlin.collections.filter' call
    var tmp0 = snapshot.n1d_1;
    // Inline function 'kotlin.collections.filterTo' call
    var destination_0 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_0 = tmp0.r();
    while (_iterator__ex2g4s_0.s()) {
      var element = _iterator__ex2g4s_0.t();
      if (!editSlotIds.v1(element.n1e_1)) {
        destination_0.w(element);
      }
    }
    // Inline function 'kotlin.collections.map' call
    // Inline function 'kotlin.collections.mapTo' call
    var destination_1 = ArrayList_init_$Create$(collectionSizeOrDefault(destination_0, 10));
    var _iterator__ex2g4s_1 = destination_0.r();
    while (_iterator__ex2g4s_1.s()) {
      var item_0 = _iterator__ex2g4s_1.t();
      var tmp$ret$8 = item_0.p1e_1;
      destination_1.w(tmp$ret$8);
    }
    // Inline function 'kotlin.collections.filter' call
    // Inline function 'kotlin.collections.filterTo' call
    var destination_2 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_2 = destination_1.r();
    while (_iterator__ex2g4s_2.s()) {
      var element_0 = _iterator__ex2g4s_2.t();
      if (!keptTemplateIds.v1(element_0)) {
        destination_2.w(element_0);
      }
    }
    // Inline function 'kotlin.collections.map' call
    var this_1 = distinct(destination_2);
    // Inline function 'kotlin.collections.mapTo' call
    var destination_3 = ArrayList_init_$Create$(collectionSizeOrDefault(this_1, 10));
    var _iterator__ex2g4s_3 = this_1.r();
    while (_iterator__ex2g4s_3.s()) {
      var item_1 = _iterator__ex2g4s_3.t();
      var tmp = WarningCode_WORKOUT_KEPT_getInstance();
      var tmp0_0 = snapshot.o1d_1;
      var tmp$ret$15;
      $l$block: {
        // Inline function 'kotlin.collections.firstOrNull' call
        var _iterator__ex2g4s_4 = tmp0_0.r();
        while (_iterator__ex2g4s_4.s()) {
          var element_1 = _iterator__ex2g4s_4.t();
          if (element_1.x1e_1 === item_1) {
            tmp$ret$15 = element_1;
            break $l$block;
          }
        }
        tmp$ret$15 = null;
      }
      var tmp0_safe_receiver = tmp$ret$15;
      var tmp$ret$14 = new PlanWarning(tmp, item_1, tmp0_safe_receiver == null ? null : tmp0_safe_receiver.y1e_1);
      destination_3.w(tmp$ret$14);
    }
    return destination_3;
  }
  function planTemplates(snapshot, edit) {
    // Inline function 'kotlin.collections.associateBy' call
    var this_0 = snapshot.o1d_1;
    var capacity = coerceAtLeast(mapCapacity(collectionSizeOrDefault(this_0, 10)), 16);
    // Inline function 'kotlin.collections.associateByTo' call
    var destination = LinkedHashMap_init_$Create$(capacity);
    var _iterator__ex2g4s = this_0.r();
    while (_iterator__ex2g4s.s()) {
      var element = _iterator__ex2g4s.t();
      var tmp$ret$2 = element.x1e_1;
      destination.h2(tmp$ret$2, element);
    }
    var snapshotTemplates = destination;
    // Inline function 'kotlin.collections.mapNotNull' call
    var tmp0 = edit.o1d_1;
    // Inline function 'kotlin.collections.mapNotNullTo' call
    var destination_0 = ArrayList_init_$Create$_0();
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_0 = tmp0.r();
    while (_iterator__ex2g4s_0.s()) {
      var element_0 = _iterator__ex2g4s_0.t();
      var old = snapshotTemplates.b2(element_0.x1e_1);
      var tmp0_safe_receiver = old == null ? new Change(ChangeKind_TEMPLATE_ADDED_getInstance(), element_0.x1e_1, element_0.y1e_1) : !sameTemplate(old, element_0) ? new Change(ChangeKind_TEMPLATE_UPDATED_getInstance(), element_0.x1e_1, element_0.y1e_1) : null;
      if (tmp0_safe_receiver == null)
        null;
      else {
        // Inline function 'kotlin.let' call
        destination_0.w(tmp0_safe_receiver);
      }
    }
    return destination_0;
  }
  function planTemplateExercises(snapshot, edit) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = edit.o1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var tmp$ret$2 = item.x1e_1;
      destination.w(tmp$ret$2);
    }
    var editTemplateIds = toSet(destination);
    // Inline function 'kotlin.collections.map' call
    var this_1 = edit.p1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(this_1, 10));
    var _iterator__ex2g4s_0 = this_1.r();
    while (_iterator__ex2g4s_0.s()) {
      var item_0 = _iterator__ex2g4s_0.t();
      var tmp$ret$5 = item_0.f1f_1;
      destination_0.w(tmp$ret$5);
    }
    var editIds = toSet(destination_0);
    // Inline function 'kotlin.collections.associateBy' call
    var this_2 = snapshot.p1d_1;
    var capacity = coerceAtLeast(mapCapacity(collectionSizeOrDefault(this_2, 10)), 16);
    // Inline function 'kotlin.collections.associateByTo' call
    var destination_1 = LinkedHashMap_init_$Create$(capacity);
    var _iterator__ex2g4s_1 = this_2.r();
    while (_iterator__ex2g4s_1.s()) {
      var element = _iterator__ex2g4s_1.t();
      var tmp$ret$8 = element.f1f_1;
      destination_1.h2(tmp$ret$8, element);
    }
    var snapshotRows = destination_1;
    // Inline function 'kotlin.collections.mapNotNull' call
    var tmp0 = edit.p1d_1;
    // Inline function 'kotlin.collections.mapNotNullTo' call
    var destination_2 = ArrayList_init_$Create$_0();
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_2 = tmp0.r();
    while (_iterator__ex2g4s_2.s()) {
      var element_0 = _iterator__ex2g4s_2.t();
      var old = snapshotRows.b2(element_0.f1f_1);
      var tmp0_safe_receiver = old == null ? new Change(ChangeKind_TEMPLATE_EXERCISE_ADDED_getInstance(), element_0.f1f_1, null) : !sameTemplateExercise(old, element_0) ? new Change(ChangeKind_TEMPLATE_EXERCISE_UPDATED_getInstance(), element_0.f1f_1, null) : null;
      if (tmp0_safe_receiver == null)
        null;
      else {
        // Inline function 'kotlin.let' call
        destination_2.w(tmp0_safe_receiver);
      }
    }
    var changed = destination_2;
    // Inline function 'kotlin.collections.filter' call
    var tmp0_0 = snapshot.p1d_1;
    // Inline function 'kotlin.collections.filterTo' call
    var destination_3 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_3 = tmp0_0.r();
    while (_iterator__ex2g4s_3.s()) {
      var element_1 = _iterator__ex2g4s_3.t();
      if (!editIds.v1(element_1.f1f_1) && editTemplateIds.v1(element_1.g1f_1)) {
        destination_3.w(element_1);
      }
    }
    // Inline function 'kotlin.collections.map' call
    // Inline function 'kotlin.collections.mapTo' call
    var destination_4 = ArrayList_init_$Create$(collectionSizeOrDefault(destination_3, 10));
    var _iterator__ex2g4s_4 = destination_3.r();
    while (_iterator__ex2g4s_4.s()) {
      var item_1 = _iterator__ex2g4s_4.t();
      var tmp$ret$21 = new Change(ChangeKind_TEMPLATE_EXERCISE_REMOVED_getInstance(), item_1.f1f_1, null);
      destination_4.w(tmp$ret$21);
    }
    var removed = destination_4;
    return plus(changed, removed);
  }
  function planSets(snapshot, edit) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = edit.p1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var tmp$ret$2 = item.f1f_1;
      destination.w(tmp$ret$2);
    }
    var editExerciseIds = toSet(destination);
    // Inline function 'kotlin.collections.map' call
    var this_1 = edit.q1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(this_1, 10));
    var _iterator__ex2g4s_0 = this_1.r();
    while (_iterator__ex2g4s_0.s()) {
      var item_0 = _iterator__ex2g4s_0.t();
      var tmp$ret$5 = item_0.u1f_1;
      destination_0.w(tmp$ret$5);
    }
    var editIds = toSet(destination_0);
    // Inline function 'kotlin.collections.associateBy' call
    var this_2 = snapshot.q1d_1;
    var capacity = coerceAtLeast(mapCapacity(collectionSizeOrDefault(this_2, 10)), 16);
    // Inline function 'kotlin.collections.associateByTo' call
    var destination_1 = LinkedHashMap_init_$Create$(capacity);
    var _iterator__ex2g4s_1 = this_2.r();
    while (_iterator__ex2g4s_1.s()) {
      var element = _iterator__ex2g4s_1.t();
      var tmp$ret$8 = element.u1f_1;
      destination_1.h2(tmp$ret$8, element);
    }
    var snapshotSets = destination_1;
    // Inline function 'kotlin.collections.mapNotNull' call
    var tmp0 = edit.q1d_1;
    // Inline function 'kotlin.collections.mapNotNullTo' call
    var destination_2 = ArrayList_init_$Create$_0();
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_2 = tmp0.r();
    while (_iterator__ex2g4s_2.s()) {
      var element_0 = _iterator__ex2g4s_2.t();
      var old = snapshotSets.b2(element_0.u1f_1);
      var tmp0_safe_receiver = old == null ? new Change(ChangeKind_SET_ADDED_getInstance(), element_0.u1f_1, null) : !sameSet(old, element_0) ? new Change(ChangeKind_SET_UPDATED_getInstance(), element_0.u1f_1, null) : null;
      if (tmp0_safe_receiver == null)
        null;
      else {
        // Inline function 'kotlin.let' call
        destination_2.w(tmp0_safe_receiver);
      }
    }
    var changed = destination_2;
    // Inline function 'kotlin.collections.filter' call
    var tmp0_0 = snapshot.q1d_1;
    // Inline function 'kotlin.collections.filterTo' call
    var destination_3 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_3 = tmp0_0.r();
    while (_iterator__ex2g4s_3.s()) {
      var element_1 = _iterator__ex2g4s_3.t();
      if (!editIds.v1(element_1.u1f_1) && editExerciseIds.v1(element_1.v1f_1)) {
        destination_3.w(element_1);
      }
    }
    // Inline function 'kotlin.collections.map' call
    // Inline function 'kotlin.collections.mapTo' call
    var destination_4 = ArrayList_init_$Create$(collectionSizeOrDefault(destination_3, 10));
    var _iterator__ex2g4s_4 = destination_3.r();
    while (_iterator__ex2g4s_4.s()) {
      var item_1 = _iterator__ex2g4s_4.t();
      var tmp$ret$21 = new Change(ChangeKind_SET_REMOVED_getInstance(), item_1.u1f_1, null);
      destination_4.w(tmp$ret$21);
    }
    var removed = destination_4;
    return plus(changed, removed);
  }
  function planExercises(snapshot, edit, warnings) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = snapshot.r1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var tmp$ret$2 = item.l1g_1;
      destination.w(tmp$ret$2);
    }
    var known = toSet(destination);
    // Inline function 'kotlin.collections.filter' call
    var tmp0 = edit.r1d_1;
    // Inline function 'kotlin.collections.filterTo' call
    var destination_0 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_0 = tmp0.r();
    while (_iterator__ex2g4s_0.s()) {
      var element = _iterator__ex2g4s_0.t();
      if (!known.v1(element.l1g_1)) {
        destination_0.w(element);
      }
    }
    var added = destination_0;
    // Inline function 'kotlin.collections.map' call
    // Inline function 'kotlin.collections.mapTo' call
    var destination_1 = ArrayList_init_$Create$(collectionSizeOrDefault(added, 10));
    var _iterator__ex2g4s_1 = added.r();
    while (_iterator__ex2g4s_1.s()) {
      var item_0 = _iterator__ex2g4s_1.t();
      var tmp$ret$8 = new PlanWarning(WarningCode_NEW_MOVEMENT_getInstance(), item_0.l1g_1, item_0.m1g_1);
      destination_1.w(tmp$ret$8);
    }
    // Inline function 'kotlin.collections.plusAssign' call
    addAll(warnings, destination_1);
    // Inline function 'kotlin.collections.map' call
    // Inline function 'kotlin.collections.mapTo' call
    var destination_2 = ArrayList_init_$Create$(collectionSizeOrDefault(added, 10));
    var _iterator__ex2g4s_2 = added.r();
    while (_iterator__ex2g4s_2.s()) {
      var item_1 = _iterator__ex2g4s_2.t();
      var tmp$ret$12 = new Change(ChangeKind_EXERCISE_ADDED_getInstance(), item_1.l1g_1, item_1.m1g_1);
      destination_2.w(tmp$ret$12);
    }
    return destination_2;
  }
  function latestWrite(document) {
    var latest = {_v: document.m1d_1.h1e_1};
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s = document.n1d_1.r();
    while (_iterator__ex2g4s.s()) {
      var element = _iterator__ex2g4s.t();
      latestWrite$consider(latest, element.t1e_1);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_0 = document.o1d_1.r();
    while (_iterator__ex2g4s_0.s()) {
      var element_0 = _iterator__ex2g4s_0.t();
      latestWrite$consider(latest, element_0.b1f_1);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_1 = document.p1d_1.r();
    while (_iterator__ex2g4s_1.s()) {
      var element_1 = _iterator__ex2g4s_1.t();
      latestWrite$consider(latest, element_1.o1f_1);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_2 = document.q1d_1.r();
    while (_iterator__ex2g4s_2.s()) {
      var element_2 = _iterator__ex2g4s_2.t();
      latestWrite$consider(latest, element_2.g1g_1);
    }
    return latest._v;
  }
  function sameSlot(a, b) {
    return a.o1e_1 === b.o1e_1 && a.p1e_1 === b.p1e_1 && a.q1e_1 === b.q1e_1 && equals(a.r1e_1, b.r1e_1);
  }
  function sameTemplate(a, b) {
    return a.y1e_1 === b.y1e_1 && a.z1e_1 == b.z1e_1;
  }
  function sameTemplateExercise(a, b) {
    return a.g1f_1 === b.g1f_1 && a.h1f_1 === b.h1f_1 && a.i1f_1 === b.i1f_1 && a.j1f_1 == b.j1f_1 && a.k1f_1 == b.k1f_1 && a.l1f_1 == b.l1f_1 && a.m1f_1 == b.m1f_1;
  }
  function sameSet(a, b) {
    return a.v1f_1 === b.v1f_1 && a.w1f_1 === b.w1f_1 && a.x1f_1.equals(b.x1f_1) && equals(a.y1f_1, b.y1f_1) && equals(a.z1f_1, b.z1f_1) && a.a1g_1 == b.a1g_1 && a.b1g_1 == b.b1g_1 && a.c1g_1 == b.c1g_1 && equals(a.d1g_1, b.d1g_1) && a.e1g_1 == b.e1g_1;
  }
  function latestWrite$consider(latest, time) {
    if (compare(time, latest._v) > 0) {
      latest._v = time;
    }
  }
  function ChangeKind_PROGRAM_RENAMED_getInstance() {
    static_init_3();
    return ChangeKind_PROGRAM_RENAMED_instance;
  }
  function ChangeKind_PROGRAM_KIND_getInstance() {
    static_init_3();
    return ChangeKind_PROGRAM_KIND_instance;
  }
  function ChangeKind_PROGRAM_START_getInstance() {
    static_init_3();
    return ChangeKind_PROGRAM_START_instance;
  }
  function ChangeKind_SLOT_ADDED_getInstance() {
    static_init_3();
    return ChangeKind_SLOT_ADDED_instance;
  }
  function ChangeKind_SLOT_UPDATED_getInstance() {
    static_init_3();
    return ChangeKind_SLOT_UPDATED_instance;
  }
  function ChangeKind_SLOT_REMOVED_getInstance() {
    static_init_3();
    return ChangeKind_SLOT_REMOVED_instance;
  }
  function ChangeKind_TEMPLATE_ADDED_getInstance() {
    static_init_3();
    return ChangeKind_TEMPLATE_ADDED_instance;
  }
  function ChangeKind_TEMPLATE_UPDATED_getInstance() {
    static_init_3();
    return ChangeKind_TEMPLATE_UPDATED_instance;
  }
  function ChangeKind_TEMPLATE_EXERCISE_ADDED_getInstance() {
    static_init_3();
    return ChangeKind_TEMPLATE_EXERCISE_ADDED_instance;
  }
  function ChangeKind_TEMPLATE_EXERCISE_UPDATED_getInstance() {
    static_init_3();
    return ChangeKind_TEMPLATE_EXERCISE_UPDATED_instance;
  }
  function ChangeKind_TEMPLATE_EXERCISE_REMOVED_getInstance() {
    static_init_3();
    return ChangeKind_TEMPLATE_EXERCISE_REMOVED_instance;
  }
  function ChangeKind_SET_ADDED_getInstance() {
    static_init_3();
    return ChangeKind_SET_ADDED_instance;
  }
  function ChangeKind_SET_UPDATED_getInstance() {
    static_init_3();
    return ChangeKind_SET_UPDATED_instance;
  }
  function ChangeKind_SET_REMOVED_getInstance() {
    static_init_3();
    return ChangeKind_SET_REMOVED_instance;
  }
  function ChangeKind_EXERCISE_ADDED_getInstance() {
    static_init_3();
    return ChangeKind_EXERCISE_ADDED_instance;
  }
  function WarningCode_NEW_MOVEMENT_getInstance() {
    static_init_4();
    return WarningCode_NEW_MOVEMENT_instance;
  }
  function WarningCode_WORKOUT_KEPT_getInstance() {
    static_init_4();
    return WarningCode_WORKOUT_KEPT_instance;
  }
  function Refusal(sink) {
    this.f1h_1 = sink;
  }
  protoOf(Refusal).g1h = function (code, path, detail) {
    this.f1h_1(code, path, detail);
  };
  protoOf(Refusal).h1h = function (code, path, detail, $super) {
    path = path === VOID ? '' : path;
    detail = detail === VOID ? null : detail;
    var tmp;
    if ($super === VOID) {
      this.g1h(code, path, detail);
      tmp = Unit_instance;
    } else {
      tmp = $super.g1h.call(this, code, path, detail);
    }
    return tmp;
  };
  function validate(document) {
    // Inline function 'kotlin.collections.mutableListOf' call
    var problems = ArrayList_init_$Create$_0();
    var refuse = new Refusal(validate$lambda(problems));
    if (isBlank(document.m1d_1.a1e_1)) {
      refuse.h1h(ProblemCode_MALFORMED_getInstance(), 'program.id');
    }
    if (isBlank(document.m1d_1.b1e_1)) {
      refuse.h1h(ProblemCode_EMPTY_NAME_getInstance(), 'program.name');
    }
    validateDuplicates(document, refuse);
    validateTemplates(document, refuse);
    validateExercises(document, refuse);
    validateSlots(document, refuse);
    validateTemplateExercises(document, refuse);
    validateSets(document, refuse);
    return problems;
  }
  function validateDuplicates(document, refuse) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = document.o1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var tmp$ret$2 = item.x1e_1;
      destination.w(tmp$ret$2);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_0 = duplicates(destination).r();
    while (_iterator__ex2g4s_0.s()) {
      var element = _iterator__ex2g4s_0.t();
      refuse.g1h(ProblemCode_DUPLICATE_ID_getInstance(), 'templates', element);
    }
    // Inline function 'kotlin.collections.map' call
    var this_1 = document.n1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(this_1, 10));
    var _iterator__ex2g4s_1 = this_1.r();
    while (_iterator__ex2g4s_1.s()) {
      var item_0 = _iterator__ex2g4s_1.t();
      var tmp$ret$7 = item_0.n1e_1;
      destination_0.w(tmp$ret$7);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_2 = duplicates(destination_0).r();
    while (_iterator__ex2g4s_2.s()) {
      var element_0 = _iterator__ex2g4s_2.t();
      refuse.g1h(ProblemCode_DUPLICATE_ID_getInstance(), 'slots', element_0);
    }
    // Inline function 'kotlin.collections.map' call
    var this_2 = document.p1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_1 = ArrayList_init_$Create$(collectionSizeOrDefault(this_2, 10));
    var _iterator__ex2g4s_3 = this_2.r();
    while (_iterator__ex2g4s_3.s()) {
      var item_1 = _iterator__ex2g4s_3.t();
      var tmp$ret$12 = item_1.f1f_1;
      destination_1.w(tmp$ret$12);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_4 = duplicates(destination_1).r();
    while (_iterator__ex2g4s_4.s()) {
      var element_1 = _iterator__ex2g4s_4.t();
      refuse.g1h(ProblemCode_DUPLICATE_ID_getInstance(), 'templateExercises', element_1);
    }
    // Inline function 'kotlin.collections.map' call
    var this_3 = document.q1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_2 = ArrayList_init_$Create$(collectionSizeOrDefault(this_3, 10));
    var _iterator__ex2g4s_5 = this_3.r();
    while (_iterator__ex2g4s_5.s()) {
      var item_2 = _iterator__ex2g4s_5.t();
      var tmp$ret$17 = item_2.u1f_1;
      destination_2.w(tmp$ret$17);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_6 = duplicates(destination_2).r();
    while (_iterator__ex2g4s_6.s()) {
      var element_2 = _iterator__ex2g4s_6.t();
      refuse.g1h(ProblemCode_DUPLICATE_ID_getInstance(), 'templateSets', element_2);
    }
    // Inline function 'kotlin.collections.map' call
    var this_4 = document.r1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_3 = ArrayList_init_$Create$(collectionSizeOrDefault(this_4, 10));
    var _iterator__ex2g4s_7 = this_4.r();
    while (_iterator__ex2g4s_7.s()) {
      var item_3 = _iterator__ex2g4s_7.t();
      var tmp$ret$22 = item_3.l1g_1;
      destination_3.w(tmp$ret$22);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_8 = duplicates(destination_3).r();
    while (_iterator__ex2g4s_8.s()) {
      var element_3 = _iterator__ex2g4s_8.t();
      refuse.g1h(ProblemCode_DUPLICATE_ID_getInstance(), 'exercises', element_3);
    }
  }
  function validateTemplates(document, refuse) {
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index = 0;
    var _iterator__ex2g4s = document.o1d_1.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var _unary__edvuaz = index;
      index = _unary__edvuaz + 1 | 0;
      var index_0 = checkIndexOverflow(_unary__edvuaz);
      if (isBlank(item.y1e_1)) {
        refuse.h1h(ProblemCode_EMPTY_NAME_getInstance(), 'templates[' + index_0 + '].name');
      }
    }
  }
  function validateExercises(document, refuse) {
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index = 0;
    var _iterator__ex2g4s = document.r1d_1.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var _unary__edvuaz = index;
      index = _unary__edvuaz + 1 | 0;
      var index_0 = checkIndexOverflow(_unary__edvuaz);
      if (isBlank(item.m1g_1)) {
        refuse.h1h(ProblemCode_EMPTY_NAME_getInstance(), 'exercises[' + index_0 + '].name');
      }
      if (!(item.z1g_1 === 'MOVEMENT') && !(item.z1g_1 === 'CATEGORY')) {
        refuse.g1h(ProblemCode_MALFORMED_getInstance(), 'exercises[' + index_0 + '].rowKind', item.z1g_1);
      }
    }
  }
  function validateSlots(document, refuse) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = document.o1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var tmp$ret$2 = item.x1e_1;
      destination.w(tmp$ret$2);
    }
    var templateIds = toSet(destination);
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index = 0;
    var _iterator__ex2g4s_0 = document.n1d_1.r();
    while (_iterator__ex2g4s_0.s()) {
      var item_0 = _iterator__ex2g4s_0.t();
      var _unary__edvuaz = index;
      index = _unary__edvuaz + 1 | 0;
      var index_0 = checkIndexOverflow(_unary__edvuaz);
      if (!(item_0.o1e_1 === document.m1d_1.a1e_1)) {
        refuse.g1h(ProblemCode_MALFORMED_getInstance(), 'slots[' + index_0 + '].programId', item_0.o1e_1);
      }
      if (!templateIds.v1(item_0.p1e_1)) {
        refuse.g1h(ProblemCode_DANGLING_SLOT_TEMPLATE_getInstance(), 'slots[' + index_0 + '].templateId', item_0.p1e_1);
      }
      if (item_0.q1e_1 < 0) {
        refuse.h1h(ProblemCode_INVALID_POSITION_getInstance(), 'slots[' + index_0 + '].position');
      }
    }
    // Inline function 'kotlin.collections.map' call
    var this_1 = document.n1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(this_1, 10));
    var _iterator__ex2g4s_1 = this_1.r();
    while (_iterator__ex2g4s_1.s()) {
      var item_1 = _iterator__ex2g4s_1.t();
      var tmp$ret$7 = item_1.q1e_1;
      destination_0.w(tmp$ret$7);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_2 = duplicates(destination_0).r();
    while (_iterator__ex2g4s_2.s()) {
      var element = _iterator__ex2g4s_2.t();
      refuse.g1h(ProblemCode_INVALID_POSITION_getInstance(), 'slots', element);
    }
  }
  function validateTemplateExercises(document, refuse) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = document.o1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var tmp$ret$2 = item.x1e_1;
      destination.w(tmp$ret$2);
    }
    var templateIds = toSet(destination);
    // Inline function 'kotlin.collections.associateBy' call
    var this_1 = document.r1d_1;
    var capacity = coerceAtLeast(mapCapacity(collectionSizeOrDefault(this_1, 10)), 16);
    // Inline function 'kotlin.collections.associateByTo' call
    var destination_0 = LinkedHashMap_init_$Create$(capacity);
    var _iterator__ex2g4s_0 = this_1.r();
    while (_iterator__ex2g4s_0.s()) {
      var element = _iterator__ex2g4s_0.t();
      var tmp$ret$5 = element.l1g_1;
      destination_0.h2(tmp$ret$5, element);
    }
    var exerciseById = destination_0;
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index = 0;
    var _iterator__ex2g4s_1 = document.p1d_1.r();
    while (_iterator__ex2g4s_1.s()) {
      var item_0 = _iterator__ex2g4s_1.t();
      var _unary__edvuaz = index;
      index = _unary__edvuaz + 1 | 0;
      var path = 'templateExercises[' + checkIndexOverflow(_unary__edvuaz) + ']';
      if (!templateIds.v1(item_0.g1f_1)) {
        refuse.g1h(ProblemCode_DANGLING_TEMPLATE_EXERCISE_getInstance(), path + '.templateId', item_0.g1f_1);
      }
      var tmp0_safe_receiver = exerciseById.b2(item_0.h1f_1);
      if (!((tmp0_safe_receiver == null ? null : tmp0_safe_receiver.z1g_1) === 'MOVEMENT')) {
        refuse.g1h(ProblemCode_DANGLING_EXERCISE_getInstance(), path + '.exerciseId', item_0.h1f_1);
      }
      if (item_0.i1f_1 < 0) {
        refuse.h1h(ProblemCode_INVALID_POSITION_getInstance(), path + '.position');
      }
      var tmp1_safe_receiver = item_0.j1f_1;
      if (tmp1_safe_receiver == null)
        null;
      else {
        // Inline function 'kotlin.let' call
        if (tmp1_safe_receiver < 0 || tmp1_safe_receiver > 3600) {
          refuse.h1h(ProblemCode_INVALID_REST_getInstance(), path + '.restSeconds');
        }
      }
      var tmp2_safe_receiver = item_0.l1f_1;
      if (tmp2_safe_receiver == null)
        null;
      else {
        // Inline function 'kotlin.let' call
        if (!(1 <= tmp2_safe_receiver ? tmp2_safe_receiver <= 20 : false)) {
          refuse.h1h(ProblemCode_INVALID_RPE_getInstance(), path + '.targetRpeHalves');
        }
      }
    }
    // Inline function 'kotlin.collections.groupBy' call
    var tmp0 = document.p1d_1;
    // Inline function 'kotlin.collections.groupByTo' call
    var destination_1 = LinkedHashMap_init_$Create$_0();
    var _iterator__ex2g4s_2 = tmp0.r();
    while (_iterator__ex2g4s_2.s()) {
      var element_0 = _iterator__ex2g4s_2.t();
      var key = element_0.g1f_1;
      // Inline function 'kotlin.collections.getOrPut' call
      var value = destination_1.b2(key);
      var tmp;
      if (value == null) {
        var answer = ArrayList_init_$Create$_0();
        destination_1.h2(key, answer);
        tmp = answer;
      } else {
        tmp = value;
      }
      var list = tmp;
      list.w(element_0);
    }
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s_3 = destination_1.e2().r();
    while (_iterator__ex2g4s_3.s()) {
      var element_1 = _iterator__ex2g4s_3.t();
      // Inline function 'kotlin.collections.component1' call
      var templateId = element_1.x1();
      // Inline function 'kotlin.collections.component2' call
      var rows = element_1.y1();
      // Inline function 'kotlin.collections.map' call
      // Inline function 'kotlin.collections.mapTo' call
      var destination_2 = ArrayList_init_$Create$(collectionSizeOrDefault(rows, 10));
      var _iterator__ex2g4s_4 = rows.r();
      while (_iterator__ex2g4s_4.s()) {
        var item_1 = _iterator__ex2g4s_4.t();
        var tmp$ret$24 = item_1.i1f_1;
        destination_2.w(tmp$ret$24);
      }
      // Inline function 'kotlin.collections.forEach' call
      var _iterator__ex2g4s_5 = duplicates(destination_2).r();
      while (_iterator__ex2g4s_5.s()) {
        var element_2 = _iterator__ex2g4s_5.t();
        refuse.g1h(ProblemCode_INVALID_POSITION_getInstance(), 'templateExercises', templateId + '/' + element_2);
      }
    }
  }
  function validateSets(document, refuse) {
    validateSetTargets(document, refuse);
    validateSetIndexes(document, refuse);
  }
  function validateSetTargets(document, refuse) {
    // Inline function 'kotlin.collections.map' call
    var this_0 = document.p1d_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var tmp$ret$2 = item.f1f_1;
      destination.w(tmp$ret$2);
    }
    var plannedIds = toSet(destination);
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index = 0;
    var _iterator__ex2g4s_0 = document.q1d_1.r();
    while (_iterator__ex2g4s_0.s()) {
      var item_0 = _iterator__ex2g4s_0.t();
      var _unary__edvuaz = index;
      index = _unary__edvuaz + 1 | 0;
      var path = 'templateSets[' + checkIndexOverflow(_unary__edvuaz) + ']';
      if (!plannedIds.v1(item_0.v1f_1)) {
        refuse.g1h(ProblemCode_DANGLING_SET_getInstance(), path + '.templateExerciseId', item_0.v1f_1);
      }
      if (item_0.w1f_1 < 0) {
        refuse.h1h(ProblemCode_INVALID_SET_INDEX_getInstance(), path + '.setIndex');
      }
      var tmp0_elvis_lhs = item_0.y1f_1;
      if (compare(tmp0_elvis_lhs == null ? new Long(0, 0) : tmp0_elvis_lhs, new Long(0, 0)) < 0) {
        refuse.h1h(ProblemCode_INVALID_LOAD_getInstance(), path + '.targetWeightGrams');
      }
      var tmp1_elvis_lhs = item_0.z1f_1;
      if (compare(tmp1_elvis_lhs == null ? new Long(0, 0) : tmp1_elvis_lhs, new Long(0, 0)) < 0) {
        refuse.h1h(ProblemCode_INVALID_LOAD_getInstance(), path + '.targetAssistanceGrams');
      }
      var tmp2_elvis_lhs = item_0.d1g_1;
      if (compare(tmp2_elvis_lhs == null ? new Long(0, 0) : tmp2_elvis_lhs, new Long(0, 0)) < 0) {
        refuse.h1h(ProblemCode_INVALID_LOAD_getInstance(), path + '.dropValueGrams');
      }
      validateReps(item_0, path, refuse);
    }
  }
  function validateReps(set, path, refuse) {
    var tmp0_safe_receiver = set.a1g_1;
    if (tmp0_safe_receiver == null)
      null;
    else {
      // Inline function 'kotlin.let' call
      if (tmp0_safe_receiver < 1) {
        refuse.h1h(ProblemCode_INVALID_REPS_getInstance(), path + '.targetRepsMin');
      }
    }
    var tmp1_safe_receiver = set.b1g_1;
    if (tmp1_safe_receiver == null)
      null;
    else {
      // Inline function 'kotlin.let' call
      if (tmp1_safe_receiver < 1) {
        refuse.h1h(ProblemCode_INVALID_REPS_getInstance(), path + '.targetRepsMax');
      }
    }
    var min = set.a1g_1;
    var max = set.b1g_1;
    if (!(min == null) && !(max == null) && min > max) {
      refuse.h1h(ProblemCode_INVALID_REPS_getInstance(), path);
    }
    var tmp2_safe_receiver = set.c1g_1;
    if (tmp2_safe_receiver == null)
      null;
    else {
      // Inline function 'kotlin.let' call
      if (tmp2_safe_receiver < 1) {
        refuse.h1h(ProblemCode_INVALID_REPS_getInstance(), path + '.targetRepsCurrent');
      }
      if (!(max == null) && tmp2_safe_receiver > max) {
        refuse.h1h(ProblemCode_INVALID_REPS_getInstance(), path + '.targetRepsCurrent');
      }
      if (!(min == null) && tmp2_safe_receiver < min) {
        refuse.h1h(ProblemCode_INVALID_REPS_getInstance(), path + '.targetRepsCurrent');
      }
    }
  }
  function validateSetIndexes(document, refuse) {
    // Inline function 'kotlin.collections.groupBy' call
    var tmp0 = document.q1d_1;
    // Inline function 'kotlin.collections.groupByTo' call
    var destination = LinkedHashMap_init_$Create$_0();
    var _iterator__ex2g4s = tmp0.r();
    while (_iterator__ex2g4s.s()) {
      var element = _iterator__ex2g4s.t();
      var key = element.v1f_1;
      // Inline function 'kotlin.collections.getOrPut' call
      var value = destination.b2(key);
      var tmp;
      if (value == null) {
        var answer = ArrayList_init_$Create$_0();
        destination.h2(key, answer);
        tmp = answer;
      } else {
        tmp = value;
      }
      var list = tmp;
      list.w(element);
    }
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s_0 = destination.e2().r();
    while (_iterator__ex2g4s_0.s()) {
      var element_0 = _iterator__ex2g4s_0.t();
      // Inline function 'kotlin.collections.component1' call
      var plannedId = element_0.x1();
      // Inline function 'kotlin.collections.component2' call
      var rows = element_0.y1();
      // Inline function 'kotlin.collections.map' call
      // Inline function 'kotlin.collections.mapTo' call
      var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(rows, 10));
      var _iterator__ex2g4s_1 = rows.r();
      while (_iterator__ex2g4s_1.s()) {
        var item = _iterator__ex2g4s_1.t();
        var tmp$ret$12 = item.w1f_1;
        destination_0.w(tmp$ret$12);
      }
      // Inline function 'kotlin.collections.forEach' call
      var _iterator__ex2g4s_2 = duplicates(destination_0).r();
      while (_iterator__ex2g4s_2.s()) {
        var element_1 = _iterator__ex2g4s_2.t();
        refuse.g1h(ProblemCode_INVALID_SET_INDEX_getInstance(), 'templateSets', plannedId + '/' + element_1);
      }
      // Inline function 'kotlin.collections.sortedBy' call
      // Inline function 'kotlin.comparisons.compareBy' call
      var tmp_0 = validateSetIndexes$lambda;
      var tmp$ret$16 = new sam$kotlin_Comparator$0(tmp_0);
      var tmp$ret$15 = sortedWith(rows, tmp$ret$16);
      validateRuns(tmp$ret$15, plannedId, refuse);
    }
  }
  function validateRuns(ordered, plannedId, refuse) {
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index = 0;
    var _iterator__ex2g4s = ordered.r();
    while (_iterator__ex2g4s.s()) {
      var item = _iterator__ex2g4s.t();
      var _unary__edvuaz = index;
      index = _unary__edvuaz + 1 | 0;
      var tmp0 = checkIndexOverflow(_unary__edvuaz);
      $l$block: {
        if (!item.x1f_1.e1h()) {
          break $l$block;
        }
        var path = 'templateSets(' + plannedId + ')[' + tmp0 + ']';
        if (carriesTargets(item)) {
          refuse.g1h(ProblemCode_RUNG_CARRIES_TARGETS_getInstance(), path, null);
        }
        var above = getOrNull(ordered, tmp0 - 1 | 0);
        if (above == null) {
          refuse.g1h(ProblemCode_RUNG_WITHOUT_ANCHOR_getInstance(), path, null);
        } else if (above.x1f_1.equals(ProgramSetRole_WARMUP_getInstance())) {
          refuse.g1h(ProblemCode_RUNG_UNDER_WARMUP_getInstance(), path, null);
        } else if (!above.x1f_1.e1h()) {
          var tmp;
          if (item.x1f_1.equals(ProgramSetRole_DROP_getInstance())) {
            var tmp0_elvis_lhs = item.d1g_1;
            tmp = compare(tmp0_elvis_lhs == null ? new Long(0, 0) : tmp0_elvis_lhs, new Long(0, 0)) <= 0;
          } else {
            tmp = false;
          }
          if (tmp) {
            refuse.g1h(ProblemCode_RUNG_WITHOUT_VALUE_getInstance(), path, null);
          }
        } else if (!above.x1f_1.equals(item.x1f_1)) {
          refuse.g1h(ProblemCode_RUN_MIXES_KINDS_getInstance(), path, null);
        }
      }
    }
  }
  function carriesTargets(_this__u8e3s4) {
    return carriesLoad(_this__u8e3s4) || carriesReps(_this__u8e3s4);
  }
  function carriesLoad(_this__u8e3s4) {
    return !(_this__u8e3s4.y1f_1 == null) || !(_this__u8e3s4.z1f_1 == null);
  }
  function carriesReps(_this__u8e3s4) {
    return !(_this__u8e3s4.a1g_1 == null) || !(_this__u8e3s4.b1g_1 == null);
  }
  function duplicates(ids) {
    // Inline function 'kotlin.collections.groupingBy' call
    var tmp$ret$0 = new duplicates$$inlined$groupingBy$1(ids);
    // Inline function 'kotlin.collections.filterValues' call
    var this_0 = eachCount(tmp$ret$0);
    var result = LinkedHashMap_init_$Create$_0();
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s = this_0.e2().r();
    while (_iterator__ex2g4s.s()) {
      var entry = _iterator__ex2g4s.t();
      if (entry.y1() > 1) {
        result.h2(entry.x1(), entry.y1());
      }
    }
    // Inline function 'kotlin.collections.map' call
    var this_1 = result.c2();
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_1, 10));
    var _iterator__ex2g4s_0 = this_1.r();
    while (_iterator__ex2g4s_0.s()) {
      var item = _iterator__ex2g4s_0.t();
      var tmp$ret$6 = toString(item);
      destination.w(tmp$ret$6);
    }
    return destination;
  }
  function sam$kotlin_Comparator$0(function_0) {
    this.i1h_1 = function_0;
  }
  protoOf(sam$kotlin_Comparator$0).t9 = function (a, b) {
    return this.i1h_1(a, b);
  };
  protoOf(sam$kotlin_Comparator$0).compare = function (a, b) {
    return this.t9(a, b);
  };
  protoOf(sam$kotlin_Comparator$0).t2 = function () {
    return this.i1h_1;
  };
  protoOf(sam$kotlin_Comparator$0).equals = function (other) {
    var tmp;
    if (!(other == null) ? isInterface(other, Comparator) : false) {
      var tmp_0;
      if (!(other == null) ? isInterface(other, FunctionAdapter) : false) {
        tmp_0 = equals(this.t2(), other.t2());
      } else {
        tmp_0 = false;
      }
      tmp = tmp_0;
    } else {
      tmp = false;
    }
    return tmp;
  };
  protoOf(sam$kotlin_Comparator$0).hashCode = function () {
    return hashCode(this.t2());
  };
  function validate$lambda($problems) {
    return function (code, path, detail) {
      var tmp0 = $problems;
      // Inline function 'kotlin.collections.plusAssign' call
      var element = new ProgramProblem(code, path, detail);
      tmp0.w(element);
      return Unit_instance;
    };
  }
  function validateSetIndexes$lambda(a, b) {
    // Inline function 'kotlin.comparisons.compareValuesBy' call
    var tmp = a.w1f_1;
    var tmp$ret$2 = b.w1f_1;
    return compareValues(tmp, tmp$ret$2);
  }
  function duplicates$$inlined$groupingBy$1($this) {
    this.j1h_1 = $this;
  }
  protoOf(duplicates$$inlined$groupingBy$1).e3 = function () {
    return this.j1h_1.r();
  };
  protoOf(duplicates$$inlined$groupingBy$1).k1h = function (element) {
    return element;
  };
  protoOf(duplicates$$inlined$groupingBy$1).f3 = function (element) {
    return this.k1h(element);
  };
  //region block: post-declaration
  protoOf($serializer).pq = typeParametersSerializers;
  protoOf($serializer_0).pq = typeParametersSerializers;
  protoOf($serializer_1).pq = typeParametersSerializers;
  protoOf($serializer_2).pq = typeParametersSerializers;
  protoOf($serializer_3).pq = typeParametersSerializers;
  protoOf($serializer_4).pq = typeParametersSerializers;
  protoOf($serializer_5).pq = typeParametersSerializers;
  protoOf($serializer_6).pq = typeParametersSerializers;
  protoOf($serializer_7).pq = typeParametersSerializers;
  protoOf($serializer_8).pq = typeParametersSerializers;
  protoOf($serializer_9).pq = typeParametersSerializers;
  protoOf($serializer_10).pq = typeParametersSerializers;
  protoOf($serializer_11).pq = typeParametersSerializers;
  //endregion
  //region block: init
  Companion_instance_0 = new Companion();
  Companion_instance_1 = new Companion_0();
  Companion_instance_2 = new Companion_1();
  //endregion
  //region block: exports
  function $jsExportAll$(_) {
    var com = _.com || (_.com = {});
    var example = com.example || (com.example = {});
    var androidapp = example.androidapp || (example.androidapp = {});
    var shared = androidapp.shared || (androidapp.shared = {});
    shared.programFormatVersion = programFormatVersion;
    shared.validateJson = validateJson;
    shared.planJson = planJson;
  }
  $jsExportAll$(_);
  //endregion
  return _;
}));

//# sourceMappingURL=workout-shared.js.map
