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
  var Enum = kotlin_kotlin.$_$.t9;
  var protoOf = kotlin_kotlin.$_$.z7;
  var initMetadataForClass = kotlin_kotlin.$_$.f7;
  var VOID = kotlin_kotlin.$_$.b;
  var getStringHashCode = kotlin_kotlin.$_$.d7;
  var Exception = kotlin_kotlin.$_$.u9;
  var Exception_init_$Init$ = kotlin_kotlin.$_$.f3;
  var captureStack = kotlin_kotlin.$_$.s6;
  var Json = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_json.$_$.d;
  var Default_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_json.$_$.a;
  var SerializationException = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.h2;
  var JsonObject = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_json.$_$.b;
  var JsonPrimitive = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_json.$_$.c;
  var get_intOrNull = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_json.$_$.e;
  var getKClass = kotlin_kotlin.$_$.k8;
  var arrayOf = kotlin_kotlin.$_$.ka;
  var createKType = kotlin_kotlin.$_$.i8;
  var serializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.l2;
  var THROW_CCE = kotlin_kotlin.$_$.z9;
  var KSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.e2;
  var isInterface = kotlin_kotlin.$_$.s7;
  var initMetadataForObject = kotlin_kotlin.$_$.k7;
  var collectionSizeOrDefault = kotlin_kotlin.$_$.c4;
  var ArrayList_init_$Create$ = kotlin_kotlin.$_$.p2;
  var listOf = kotlin_kotlin.$_$.f5;
  var noWhenBranchMatchedException = kotlin_kotlin.$_$.ta;
  var initMetadataForCompanion = kotlin_kotlin.$_$.g7;
  var PluginGeneratedSerialDescriptor = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.u1;
  var StringSerializer_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.l;
  var UnknownFieldException_init_$Create$ = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.o;
  var get_nullable = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.r;
  var typeParametersSerializers = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.q1;
  var GeneratedSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.r1;
  var throwMissingFieldException = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.z1;
  var objectCreate = kotlin_kotlin.$_$.y7;
  var ArrayListSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.o1;
  var LazyThreadSafetyMode_PUBLICATION_getInstance = kotlin_kotlin.$_$.u1;
  var lazy = kotlin_kotlin.$_$.ra;
  var toString = kotlin_kotlin.$_$.b8;
  var hashCode = kotlin_kotlin.$_$.e7;
  var equals = kotlin_kotlin.$_$.a7;
  var emptyList = kotlin_kotlin.$_$.s4;
  var BooleanSerializer_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.i;
  var getBooleanHashCode = kotlin_kotlin.$_$.b7;
  var SerializableWith = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.g2;
  var Long = kotlin_kotlin.$_$.w9;
  var IntSerializer_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.j;
  var LongSerializer_getInstance = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.k;
  var toString_0 = kotlin_kotlin.$_$.va;
  var equalsLong = kotlin_kotlin.$_$.f6;
  var createSimpleEnumSerializer = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.w1;
  var Companion_instance = kotlin_kotlin.$_$.h2;
  var SerializerFactory = kotlin_org_jetbrains_kotlinx_kotlinx_serialization_core.$_$.v1;
  var compare = kotlin_kotlin.$_$.b6;
  var ArrayList_init_$Create$_0 = kotlin_kotlin.$_$.q2;
  var mapCapacity = kotlin_kotlin.$_$.g5;
  var coerceAtLeast = kotlin_kotlin.$_$.d8;
  var LinkedHashMap_init_$Create$ = kotlin_kotlin.$_$.y2;
  var toSet = kotlin_kotlin.$_$.s5;
  var distinct = kotlin_kotlin.$_$.q4;
  var isBlank = kotlin_kotlin.$_$.s8;
  var checkIndexOverflow = kotlin_kotlin.$_$.b4;
  var LinkedHashMap_init_$Create$_0 = kotlin_kotlin.$_$.z2;
  var sortedWith = kotlin_kotlin.$_$.m5;
  var getOrNull = kotlin_kotlin.$_$.v4;
  var eachCount = kotlin_kotlin.$_$.r4;
  var FunctionAdapter = kotlin_kotlin.$_$.p6;
  var Comparator = kotlin_kotlin.$_$.q9;
  var compareValues = kotlin_kotlin.$_$.v5;
  var constructCallableReference = kotlin_kotlin.$_$.y6;
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
  initMetadataForObject($serializer_6, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(ProgramDto, 'ProgramDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_6});
  initMetadataForCompanion(Companion_5);
  initMetadataForObject($serializer_7, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(ProgramSlotDto, 'ProgramSlotDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_7});
  initMetadataForObject($serializer_8, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(TemplateDto, 'TemplateDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_8});
  initMetadataForObject($serializer_9, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(TemplateExerciseDto, 'TemplateExerciseDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_9});
  initMetadataForCompanion(Companion_6);
  initMetadataForObject($serializer_10, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(TemplateSetDto, 'TemplateSetDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_10});
  initMetadataForCompanion(Companion_7);
  initMetadataForObject($serializer_11, '$serializer', VOID, VOID, [GeneratedSerializer]);
  initMetadataForClass(ExerciseDto, 'ExerciseDto', VOID, VOID, VOID, VOID, VOID, {0: $serializer_getInstance_11});
  initMetadataForCompanion(Companion_8, VOID, [SerializerFactory]);
  initMetadataForClass(Weekday, 'Weekday', VOID, Enum, VOID, VOID, VOID, {0: Companion_getInstance_8});
  initMetadataForCompanion(Companion_9, VOID, [SerializerFactory]);
  initMetadataForClass(ProgramSetRole, 'ProgramSetRole', VOID, Enum, VOID, VOID, VOID, {0: Companion_getInstance_9});
  initMetadataForClass(ChangeKind, 'ChangeKind', VOID, Enum);
  initMetadataForClass(Change, 'Change');
  initMetadataForClass(WarningCode, 'WarningCode', VOID, Enum);
  initMetadataForClass(PlanWarning, 'PlanWarning');
  initMetadataForClass(Ready, 'Ready');
  initMetadataForClass(Refused, 'Refused');
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
    this.code_1 = code;
    this.path_1 = path;
    this.detail_1 = detail;
  }
  protoOf(ProgramProblem).toString = function () {
    return 'ProgramProblem(code=' + this.code_1.toString() + ', path=' + this.path_1 + ', detail=' + this.detail_1 + ')';
  };
  protoOf(ProgramProblem).hashCode = function () {
    var result = this.code_1.hashCode();
    result = imul(result, 31) + getStringHashCode(this.path_1) | 0;
    result = imul(result, 31) + (this.detail_1 == null ? 0 : getStringHashCode(this.detail_1)) | 0;
    return result;
  };
  protoOf(ProgramProblem).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ProgramProblem))
      return false;
    if (!this.code_1.equals(other.code_1))
      return false;
    if (!(this.path_1 === other.path_1))
      return false;
    if (!(this.detail_1 == other.detail_1))
      return false;
    return true;
  };
  function ProgramFormatException(problem) {
    Exception_init_$Init$(problem.code_1.name_1, this);
    captureStack(this, ProgramFormatException);
    this.problem_1 = problem;
  }
  function ProgramDocumentCodec$json$lambda($this$Json) {
    $this$Json.prettyPrint_1 = true;
    $this$Json.ignoreUnknownKeys_1 = true;
    $this$Json.encodeDefaults_1 = true;
    return Unit_instance;
  }
  function ProgramDocumentCodec() {
    ProgramDocumentCodec_instance = this;
    this.CURRENT_FORMAT_VERSION_1 = 4;
    this.MINIMUM_FORMAT_VERSION_1 = 4;
    var tmp = this;
    tmp.json_1 = Json(VOID, ProgramDocumentCodec$json$lambda);
  }
  protoOf(ProgramDocumentCodec).decode_gmof1w_k$ = function (text) {
    var tmp;
    try {
      tmp = Default_getInstance().parseToJsonElement_rqvr2k_k$(text);
    } catch ($p) {
      var tmp_0;
      if ($p instanceof SerializationException) {
        var malformed = $p;
        throw new ProgramFormatException(new ProgramProblem(ProblemCode_MALFORMED_getInstance()));
      } else {
        throw $p;
      }
    }
    var element = tmp;
    var tmp0_safe_receiver = element instanceof JsonObject ? element : null;
    var tmp_1 = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.get_6bo4tg_k$('formatVersion');
    var tmp1_safe_receiver = tmp_1 instanceof JsonPrimitive ? tmp_1 : null;
    var version = tmp1_safe_receiver == null ? null : get_intOrNull(tmp1_safe_receiver);
    if (version == null)
      throw new ProgramFormatException(new ProgramProblem(ProblemCode_MISSING_VERSION_getInstance()));
    else if (version > 4)
      throw new ProgramFormatException(new ProgramProblem(ProblemCode_NEWER_VERSION_getInstance(), VOID, version.toString()));
    else if (version < 4)
      throw new ProgramFormatException(new ProgramProblem(ProblemCode_OLDER_VERSION_getInstance(), VOID, version.toString()));
    var tmp_2;
    try {
      // Inline function 'kotlinx.serialization.json.decodeFromJsonElement' call
      var this_0 = this.json_1;
      // Inline function 'kotlinx.serialization.serializer' call
      var this_1 = this_0.get_serializersModule_piitvg_k$();
      // Inline function 'kotlinx.serialization.internal.cast' call
      var this_2 = serializer(this_1, createKType(getKClass(ProgramDocument), arrayOf([]), false));
      var tmp$ret$1 = isInterface(this_2, KSerializer) ? this_2 : THROW_CCE();
      tmp_2 = this_0.decodeFromJsonElement_tsogwj_k$(tmp$ret$1, element);
    } catch ($p_0) {
      var tmp_3;
      if ($p_0 instanceof SerializationException) {
        var malformed_0 = $p_0;
        throw new ProgramFormatException(new ProgramProblem(ProblemCode_MALFORMED_getInstance()));
      } else {
        throw $p_0;
      }
    }
    return tmp_2;
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
    return 4;
  }
  function validateJson(documentJson) {
    _init_properties_ProgramJson_kt__8o3jdm();
    var tmp;
    try {
      // Inline function 'kotlin.collections.map' call
      var this_0 = validate(ProgramDocumentCodec_getInstance().decode_gmof1w_k$(documentJson));
      // Inline function 'kotlin.collections.mapTo' call
      var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
      var _iterator__ex2g4s = this_0.iterator_jk1svi_k$();
      while (_iterator__ex2g4s.hasNext_bitz1p_k$()) {
        var item = _iterator__ex2g4s.next_20eer_k$();
        var tmp$ret$2 = toOut(item);
        destination.add_utx5q5_k$(tmp$ret$2);
      }
      tmp = destination;
    } catch ($p) {
      var tmp_0;
      if ($p instanceof ProgramFormatException) {
        var refused = $p;
        tmp_0 = listOf(toOut(refused.problem_1));
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
    var this_1 = tmp0.get_serializersModule_piitvg_k$();
    // Inline function 'kotlinx.serialization.internal.cast' call
    var this_2 = serializer(this_1, createKType(getKClass(ValidateOut), arrayOf([]), false));
    var tmp$ret$4 = isInterface(this_2, KSerializer) ? this_2 : THROW_CCE();
    return tmp0.encodeToString_k0apqx_k$(tmp$ret$4, value);
  }
  function planJson(snapshotJson, editJson) {
    _init_properties_ProgramJson_kt__8o3jdm();
    var tmp;
    try {
      tmp = plan(ProgramDocumentCodec_getInstance().decode_gmof1w_k$(snapshotJson), ProgramDocumentCodec_getInstance().decode_gmof1w_k$(editJson));
    } catch ($p) {
      var tmp_0;
      if ($p instanceof ProgramFormatException) {
        var refused = $p;
        var tmp0 = get_json();
        // Inline function 'kotlinx.serialization.json.Json.encodeToString' call
        var value = new PlanOut(false, VOID, VOID, listOf(toOut(refused.problem_1)));
        // Inline function 'kotlinx.serialization.serializer' call
        var this_0 = tmp0.get_serializersModule_piitvg_k$();
        // Inline function 'kotlinx.serialization.internal.cast' call
        var this_1 = serializer(this_0, createKType(getKClass(PlanOut), arrayOf([]), false));
        var tmp$ret$1 = isInterface(this_1, KSerializer) ? this_1 : THROW_CCE();
        return tmp0.encodeToString_k0apqx_k$(tmp$ret$1, value);
      } else {
        throw $p;
      }
    }
    var result = tmp;
    var tmp_1;
    if (result instanceof Refused) {
      var tmp0_0 = get_json();
      // Inline function 'kotlin.collections.map' call
      var this_2 = result.problems_1;
      // Inline function 'kotlin.collections.mapTo' call
      var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_2, 10));
      var _iterator__ex2g4s = this_2.iterator_jk1svi_k$();
      while (_iterator__ex2g4s.hasNext_bitz1p_k$()) {
        var item = _iterator__ex2g4s.next_20eer_k$();
        var tmp$ret$5 = toOut(item);
        destination.add_utx5q5_k$(tmp$ret$5);
      }
      // Inline function 'kotlinx.serialization.json.Json.encodeToString' call
      var value_0 = new PlanOut(false, VOID, VOID, destination);
      // Inline function 'kotlinx.serialization.serializer' call
      var this_3 = tmp0_0.get_serializersModule_piitvg_k$();
      // Inline function 'kotlinx.serialization.internal.cast' call
      var this_4 = serializer(this_3, createKType(getKClass(PlanOut), arrayOf([]), false));
      var tmp$ret$7 = isInterface(this_4, KSerializer) ? this_4 : THROW_CCE();
      tmp_1 = tmp0_0.encodeToString_k0apqx_k$(tmp$ret$7, value_0);
    } else {
      if (result instanceof Ready) {
        var tmp0_1 = get_json();
        // Inline function 'kotlin.collections.map' call
        var this_5 = result.changes_1;
        // Inline function 'kotlin.collections.mapTo' call
        var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(this_5, 10));
        var _iterator__ex2g4s_0 = this_5.iterator_jk1svi_k$();
        while (_iterator__ex2g4s_0.hasNext_bitz1p_k$()) {
          var item_0 = _iterator__ex2g4s_0.next_20eer_k$();
          var tmp$ret$11 = new ChangeOut(item_0.kind_1.name_1, item_0.id_1, item_0.name_1);
          destination_0.add_utx5q5_k$(tmp$ret$11);
        }
        var tmp_2 = destination_0;
        // Inline function 'kotlin.collections.map' call
        var this_6 = result.warnings_1;
        // Inline function 'kotlin.collections.mapTo' call
        var destination_1 = ArrayList_init_$Create$(collectionSizeOrDefault(this_6, 10));
        var _iterator__ex2g4s_1 = this_6.iterator_jk1svi_k$();
        while (_iterator__ex2g4s_1.hasNext_bitz1p_k$()) {
          var item_1 = _iterator__ex2g4s_1.next_20eer_k$();
          var tmp$ret$14 = new WarningOut(item_1.code_1.name_1, item_1.id_1, item_1.name_1);
          destination_1.add_utx5q5_k$(tmp$ret$14);
        }
        // Inline function 'kotlinx.serialization.json.Json.encodeToString' call
        var value_1 = new PlanOut(true, tmp_2, destination_1);
        // Inline function 'kotlinx.serialization.serializer' call
        var this_7 = tmp0_1.get_serializersModule_piitvg_k$();
        // Inline function 'kotlinx.serialization.internal.cast' call
        var this_8 = serializer(this_7, createKType(getKClass(PlanOut), arrayOf([]), false));
        var tmp$ret$16 = isInterface(this_8, KSerializer) ? this_8 : THROW_CCE();
        tmp_1 = tmp0_1.encodeToString_k0apqx_k$(tmp$ret$16, value_1);
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
    tmp0_serialDesc.addElement_5pzumi_k$('code', false);
    tmp0_serialDesc.addElement_5pzumi_k$('path', false);
    tmp0_serialDesc.addElement_5pzumi_k$('detail', true);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer).serialize_75di7i_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 0, value.code_1);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 1, value.path_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 2) ? true : !(value.detail_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 2, StringSerializer_getInstance(), value.detail_1);
    }
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_75di7i_k$(encoder, value instanceof ProblemOut ? value : THROW_CCE());
  };
  protoOf($serializer).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    if (tmp7_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp7_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp7_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp7_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp7_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp7_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp7_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp7_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp7_input.endStructure_1xqz0n_k$(tmp0_desc);
    return ProblemOut_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, null);
  };
  protoOf($serializer).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer).childSerializers_5ghqw5_k$ = function () {
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
      throwMissingFieldException(seen0, 3, $serializer_getInstance().descriptor_1);
    }
    $this.code_1 = code;
    $this.path_1 = path;
    if (0 === (seen0 & 4))
      $this.detail_1 = null;
    else
      $this.detail_1 = detail;
    return $this;
  }
  function ProblemOut_init_$Create$(seen0, code, path, detail, serializationConstructorMarker) {
    return ProblemOut_init_$Init$(seen0, code, path, detail, serializationConstructorMarker, objectCreate(protoOf(ProblemOut)));
  }
  function ProblemOut(code, path, detail) {
    detail = detail === VOID ? null : detail;
    this.code_1 = code;
    this.path_1 = path;
    this.detail_1 = detail;
  }
  protoOf(ProblemOut).toString = function () {
    return 'ProblemOut(code=' + this.code_1 + ', path=' + this.path_1 + ', detail=' + this.detail_1 + ')';
  };
  protoOf(ProblemOut).hashCode = function () {
    var result = getStringHashCode(this.code_1);
    result = imul(result, 31) + getStringHashCode(this.path_1) | 0;
    result = imul(result, 31) + (this.detail_1 == null ? 0 : getStringHashCode(this.detail_1)) | 0;
    return result;
  };
  protoOf(ProblemOut).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ProblemOut))
      return false;
    if (!(this.code_1 === other.code_1))
      return false;
    if (!(this.path_1 === other.path_1))
      return false;
    if (!(this.detail_1 == other.detail_1))
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
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.ChangeOut', this, 3);
    tmp0_serialDesc.addElement_5pzumi_k$('kind', false);
    tmp0_serialDesc.addElement_5pzumi_k$('id', false);
    tmp0_serialDesc.addElement_5pzumi_k$('name', true);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_0).serialize_9drw3r_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 0, value.kind_1);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 1, value.id_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 2) ? true : !(value.name_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 2, StringSerializer_getInstance(), value.name_1);
    }
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_0).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_9drw3r_k$(encoder, value instanceof ChangeOut ? value : THROW_CCE());
  };
  protoOf($serializer_0).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    if (tmp7_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp7_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp7_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp7_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp7_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp7_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp7_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp7_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp7_input.endStructure_1xqz0n_k$(tmp0_desc);
    return ChangeOut_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, null);
  };
  protoOf($serializer_0).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_0).childSerializers_5ghqw5_k$ = function () {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), get_nullable(StringSerializer_getInstance())];
  };
  var $serializer_instance_0;
  function $serializer_getInstance_0() {
    if ($serializer_instance_0 == null)
      new $serializer_0();
    return $serializer_instance_0;
  }
  function ChangeOut_init_$Init$(seen0, kind, id, name, serializationConstructorMarker, $this) {
    if (!(3 === (3 & seen0))) {
      throwMissingFieldException(seen0, 3, $serializer_getInstance_0().descriptor_1);
    }
    $this.kind_1 = kind;
    $this.id_1 = id;
    if (0 === (seen0 & 4))
      $this.name_1 = null;
    else
      $this.name_1 = name;
    return $this;
  }
  function ChangeOut_init_$Create$(seen0, kind, id, name, serializationConstructorMarker) {
    return ChangeOut_init_$Init$(seen0, kind, id, name, serializationConstructorMarker, objectCreate(protoOf(ChangeOut)));
  }
  function ChangeOut(kind, id, name) {
    name = name === VOID ? null : name;
    this.kind_1 = kind;
    this.id_1 = id;
    this.name_1 = name;
  }
  protoOf(ChangeOut).toString = function () {
    return 'ChangeOut(kind=' + this.kind_1 + ', id=' + this.id_1 + ', name=' + this.name_1 + ')';
  };
  protoOf(ChangeOut).hashCode = function () {
    var result = getStringHashCode(this.kind_1);
    result = imul(result, 31) + getStringHashCode(this.id_1) | 0;
    result = imul(result, 31) + (this.name_1 == null ? 0 : getStringHashCode(this.name_1)) | 0;
    return result;
  };
  protoOf(ChangeOut).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ChangeOut))
      return false;
    if (!(this.kind_1 === other.kind_1))
      return false;
    if (!(this.id_1 === other.id_1))
      return false;
    if (!(this.name_1 == other.name_1))
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
    tmp0_serialDesc.addElement_5pzumi_k$('code', false);
    tmp0_serialDesc.addElement_5pzumi_k$('id', true);
    tmp0_serialDesc.addElement_5pzumi_k$('name', true);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_1).serialize_rn7xv5_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 0, value.code_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 1) ? true : !(value.id_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 1, StringSerializer_getInstance(), value.id_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 2) ? true : !(value.name_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 2, StringSerializer_getInstance(), value.name_1);
    }
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_1).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_rn7xv5_k$(encoder, value instanceof WarningOut ? value : THROW_CCE());
  };
  protoOf($serializer_1).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    if (tmp7_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp7_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp7_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 1, StringSerializer_getInstance(), tmp5_local1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp7_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp7_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp7_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp7_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 1, StringSerializer_getInstance(), tmp5_local1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp7_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp7_input.endStructure_1xqz0n_k$(tmp0_desc);
    return WarningOut_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, null);
  };
  protoOf($serializer_1).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_1).childSerializers_5ghqw5_k$ = function () {
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
      throwMissingFieldException(seen0, 1, $serializer_getInstance_1().descriptor_1);
    }
    $this.code_1 = code;
    if (0 === (seen0 & 2))
      $this.id_1 = null;
    else
      $this.id_1 = id;
    if (0 === (seen0 & 4))
      $this.name_1 = null;
    else
      $this.name_1 = name;
    return $this;
  }
  function WarningOut_init_$Create$(seen0, code, id, name, serializationConstructorMarker) {
    return WarningOut_init_$Init$(seen0, code, id, name, serializationConstructorMarker, objectCreate(protoOf(WarningOut)));
  }
  function WarningOut(code, id, name) {
    id = id === VOID ? null : id;
    name = name === VOID ? null : name;
    this.code_1 = code;
    this.id_1 = id;
    this.name_1 = name;
  }
  protoOf(WarningOut).toString = function () {
    return 'WarningOut(code=' + this.code_1 + ', id=' + this.id_1 + ', name=' + this.name_1 + ')';
  };
  protoOf(WarningOut).hashCode = function () {
    var result = getStringHashCode(this.code_1);
    result = imul(result, 31) + (this.id_1 == null ? 0 : getStringHashCode(this.id_1)) | 0;
    result = imul(result, 31) + (this.name_1 == null ? 0 : getStringHashCode(this.name_1)) | 0;
    return result;
  };
  protoOf(WarningOut).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof WarningOut))
      return false;
    if (!(this.code_1 === other.code_1))
      return false;
    if (!(this.id_1 == other.id_1))
      return false;
    if (!(this.name_1 == other.name_1))
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
    tmp.$childSerializers_1 = [lazy(tmp_0, ValidateOut$Companion$$childSerializers$_anonymous__swzrou)];
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
    tmp0_serialDesc.addElement_5pzumi_k$('problems', false);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_2).serialize_w3ybf7_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp2_cached = Companion_getInstance_2().$childSerializers_1;
    tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 0, tmp2_cached[0].get_value_j01efc_k$(), value.problems_1);
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_2).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_w3ybf7_k$(encoder, value instanceof ValidateOut ? value : THROW_CCE());
  };
  protoOf($serializer_2).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp6_cached = Companion_getInstance_2().$childSerializers_1;
    if (tmp5_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp5_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 0, tmp6_cached[0].get_value_j01efc_k$(), tmp4_local0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp5_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp5_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 0, tmp6_cached[0].get_value_j01efc_k$(), tmp4_local0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp5_input.endStructure_1xqz0n_k$(tmp0_desc);
    return ValidateOut_init_$Create$(tmp3_bitMask0, tmp4_local0, null);
  };
  protoOf($serializer_2).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_2).childSerializers_5ghqw5_k$ = function () {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [Companion_getInstance_2().$childSerializers_1[0].get_value_j01efc_k$()];
  };
  var $serializer_instance_2;
  function $serializer_getInstance_2() {
    if ($serializer_instance_2 == null)
      new $serializer_2();
    return $serializer_instance_2;
  }
  function ValidateOut_init_$Init$(seen0, problems, serializationConstructorMarker, $this) {
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance_2().descriptor_1);
    }
    $this.problems_1 = problems;
    return $this;
  }
  function ValidateOut_init_$Create$(seen0, problems, serializationConstructorMarker) {
    return ValidateOut_init_$Init$(seen0, problems, serializationConstructorMarker, objectCreate(protoOf(ValidateOut)));
  }
  function ValidateOut(problems) {
    Companion_getInstance_2();
    this.problems_1 = problems;
  }
  protoOf(ValidateOut).toString = function () {
    return 'ValidateOut(problems=' + toString(this.problems_1) + ')';
  };
  protoOf(ValidateOut).hashCode = function () {
    return hashCode(this.problems_1);
  };
  protoOf(ValidateOut).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ValidateOut))
      return false;
    if (!equals(this.problems_1, other.problems_1))
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
    tmp.$childSerializers_1 = [null, tmp_1, tmp_3, lazy(tmp_4, PlanOut$Companion$$childSerializers$_anonymous__dlb285_1)];
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
    tmp0_serialDesc.addElement_5pzumi_k$('ok', false);
    tmp0_serialDesc.addElement_5pzumi_k$('changes', true);
    tmp0_serialDesc.addElement_5pzumi_k$('warnings', true);
    tmp0_serialDesc.addElement_5pzumi_k$('problems', true);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_3).serialize_2fyv7k_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp2_cached = Companion_getInstance_3().$childSerializers_1;
    tmp1_output.encodeBooleanElement_ydht7q_k$(tmp0_desc, 0, value.ok_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 1) ? true : !equals(value.changes_1, emptyList())) {
      tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 1, tmp2_cached[1].get_value_j01efc_k$(), value.changes_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 2) ? true : !equals(value.warnings_1, emptyList())) {
      tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 2, tmp2_cached[2].get_value_j01efc_k$(), value.warnings_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 3) ? true : !equals(value.problems_1, emptyList())) {
      tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 3, tmp2_cached[3].get_value_j01efc_k$(), value.problems_1);
    }
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_3).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_2fyv7k_k$(encoder, value instanceof PlanOut ? value : THROW_CCE());
  };
  protoOf($serializer_3).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = false;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_local3 = null;
    var tmp8_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp9_cached = Companion_getInstance_3().$childSerializers_1;
    if (tmp8_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp8_input.decodeBooleanElement_vuyhtj_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp8_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 1, tmp9_cached[1].get_value_j01efc_k$(), tmp5_local1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp8_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 2, tmp9_cached[2].get_value_j01efc_k$(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp8_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 3, tmp9_cached[3].get_value_j01efc_k$(), tmp7_local3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp8_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp8_input.decodeBooleanElement_vuyhtj_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp8_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 1, tmp9_cached[1].get_value_j01efc_k$(), tmp5_local1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp8_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 2, tmp9_cached[2].get_value_j01efc_k$(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp8_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 3, tmp9_cached[3].get_value_j01efc_k$(), tmp7_local3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp8_input.endStructure_1xqz0n_k$(tmp0_desc);
    return PlanOut_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, null);
  };
  protoOf($serializer_3).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_3).childSerializers_5ghqw5_k$ = function () {
    var tmp0_cached = Companion_getInstance_3().$childSerializers_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [BooleanSerializer_getInstance(), tmp0_cached[1].get_value_j01efc_k$(), tmp0_cached[2].get_value_j01efc_k$(), tmp0_cached[3].get_value_j01efc_k$()];
  };
  var $serializer_instance_3;
  function $serializer_getInstance_3() {
    if ($serializer_instance_3 == null)
      new $serializer_3();
    return $serializer_instance_3;
  }
  function PlanOut_init_$Init$(seen0, ok, changes, warnings, problems, serializationConstructorMarker, $this) {
    if (!(1 === (1 & seen0))) {
      throwMissingFieldException(seen0, 1, $serializer_getInstance_3().descriptor_1);
    }
    $this.ok_1 = ok;
    if (0 === (seen0 & 2))
      $this.changes_1 = emptyList();
    else
      $this.changes_1 = changes;
    if (0 === (seen0 & 4))
      $this.warnings_1 = emptyList();
    else
      $this.warnings_1 = warnings;
    if (0 === (seen0 & 8))
      $this.problems_1 = emptyList();
    else
      $this.problems_1 = problems;
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
    this.ok_1 = ok;
    this.changes_1 = changes;
    this.warnings_1 = warnings;
    this.problems_1 = problems;
  }
  protoOf(PlanOut).toString = function () {
    return 'PlanOut(ok=' + this.ok_1 + ', changes=' + toString(this.changes_1) + ', warnings=' + toString(this.warnings_1) + ', problems=' + toString(this.problems_1) + ')';
  };
  protoOf(PlanOut).hashCode = function () {
    var result = getBooleanHashCode(this.ok_1);
    result = imul(result, 31) + hashCode(this.changes_1) | 0;
    result = imul(result, 31) + hashCode(this.warnings_1) | 0;
    result = imul(result, 31) + hashCode(this.problems_1) | 0;
    return result;
  };
  protoOf(PlanOut).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof PlanOut))
      return false;
    if (!(this.ok_1 === other.ok_1))
      return false;
    if (!equals(this.changes_1, other.changes_1))
      return false;
    if (!equals(this.warnings_1, other.warnings_1))
      return false;
    if (!equals(this.problems_1, other.problems_1))
      return false;
    return true;
  };
  function toOut(_this__u8e3s4) {
    _init_properties_ProgramJson_kt__8o3jdm();
    return new ProblemOut(_this__u8e3s4.code_1.name_1, _this__u8e3s4.path_1, _this__u8e3s4.detail_1);
  }
  function json$lambda($this$Json) {
    _init_properties_ProgramJson_kt__8o3jdm();
    $this$Json.encodeDefaults_1 = true;
    $this$Json.ignoreUnknownKeys_1 = true;
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
    tmp.$childSerializers_1 = [null, null, null, tmp_1, tmp_3, tmp_5, tmp_7, lazy(tmp_8, ProgramDocument$Companion$$childSerializers$_anonymous__p8b42d_3), null];
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
    tmp0_serialDesc.addElement_5pzumi_k$('formatVersion', false);
    tmp0_serialDesc.addElement_5pzumi_k$('exportedAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('program', false);
    tmp0_serialDesc.addElement_5pzumi_k$('slots', true);
    tmp0_serialDesc.addElement_5pzumi_k$('templates', true);
    tmp0_serialDesc.addElement_5pzumi_k$('templateExercises', true);
    tmp0_serialDesc.addElement_5pzumi_k$('templateSets', true);
    tmp0_serialDesc.addElement_5pzumi_k$('exercises', true);
    tmp0_serialDesc.addElement_5pzumi_k$('edit', true);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_4).serialize_sw7m5i_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp2_cached = Companion_getInstance_4().$childSerializers_1;
    tmp1_output.encodeIntElement_krhhce_k$(tmp0_desc, 0, value.formatVersion_1);
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 1, value.exportedAt_1);
    tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 2, $serializer_getInstance_6(), value.program_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 3) ? true : !equals(value.slots_1, emptyList())) {
      tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 3, tmp2_cached[3].get_value_j01efc_k$(), value.slots_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 4) ? true : !equals(value.templates_1, emptyList())) {
      tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 4, tmp2_cached[4].get_value_j01efc_k$(), value.templates_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 5) ? true : !equals(value.templateExercises_1, emptyList())) {
      tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 5, tmp2_cached[5].get_value_j01efc_k$(), value.templateExercises_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 6) ? true : !equals(value.templateSets_1, emptyList())) {
      tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 6, tmp2_cached[6].get_value_j01efc_k$(), value.templateSets_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 7) ? true : !equals(value.exercises_1, emptyList())) {
      tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 7, tmp2_cached[7].get_value_j01efc_k$(), value.exercises_1);
    }
    if (!(value.edit_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 8, $serializer_getInstance_5(), value.edit_1);
    }
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_4).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_sw7m5i_k$(encoder, value instanceof ProgramDocument ? value : THROW_CCE());
  };
  protoOf($serializer_4).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
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
    var tmp13_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp14_cached = Companion_getInstance_4().$childSerializers_1;
    if (tmp13_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp13_input.decodeIntElement_941u6a_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp13_input.decodeLongElement_994anb_k$(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 2, $serializer_getInstance_6(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 3, tmp14_cached[3].get_value_j01efc_k$(), tmp7_local3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 4, tmp14_cached[4].get_value_j01efc_k$(), tmp8_local4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 5, tmp14_cached[5].get_value_j01efc_k$(), tmp9_local5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 6, tmp14_cached[6].get_value_j01efc_k$(), tmp10_local6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
      tmp11_local7 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 7, tmp14_cached[7].get_value_j01efc_k$(), tmp11_local7);
      tmp3_bitMask0 = tmp3_bitMask0 | 128;
      tmp12_local8 = tmp13_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 8, $serializer_getInstance_5(), tmp12_local8);
      tmp3_bitMask0 = tmp3_bitMask0 | 256;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp13_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp13_input.decodeIntElement_941u6a_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp13_input.decodeLongElement_994anb_k$(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 2, $serializer_getInstance_6(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 3, tmp14_cached[3].get_value_j01efc_k$(), tmp7_local3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 4, tmp14_cached[4].get_value_j01efc_k$(), tmp8_local4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 5, tmp14_cached[5].get_value_j01efc_k$(), tmp9_local5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 6, tmp14_cached[6].get_value_j01efc_k$(), tmp10_local6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          case 7:
            tmp11_local7 = tmp13_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 7, tmp14_cached[7].get_value_j01efc_k$(), tmp11_local7);
            tmp3_bitMask0 = tmp3_bitMask0 | 128;
            break;
          case 8:
            tmp12_local8 = tmp13_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 8, $serializer_getInstance_5(), tmp12_local8);
            tmp3_bitMask0 = tmp3_bitMask0 | 256;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp13_input.endStructure_1xqz0n_k$(tmp0_desc);
    return ProgramDocument_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, tmp11_local7, tmp12_local8, null);
  };
  protoOf($serializer_4).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_4).childSerializers_5ghqw5_k$ = function () {
    var tmp0_cached = Companion_getInstance_4().$childSerializers_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [IntSerializer_getInstance(), LongSerializer_getInstance(), $serializer_getInstance_6(), tmp0_cached[3].get_value_j01efc_k$(), tmp0_cached[4].get_value_j01efc_k$(), tmp0_cached[5].get_value_j01efc_k$(), tmp0_cached[6].get_value_j01efc_k$(), tmp0_cached[7].get_value_j01efc_k$(), get_nullable($serializer_getInstance_5())];
  };
  var $serializer_instance_4;
  function $serializer_getInstance_4() {
    if ($serializer_instance_4 == null)
      new $serializer_4();
    return $serializer_instance_4;
  }
  function ProgramDocument_init_$Init$(seen0, formatVersion, exportedAt, program, slots, templates, templateExercises, templateSets, exercises, edit, serializationConstructorMarker, $this) {
    if (!(7 === (7 & seen0))) {
      throwMissingFieldException(seen0, 7, $serializer_getInstance_4().descriptor_1);
    }
    $this.formatVersion_1 = formatVersion;
    $this.exportedAt_1 = exportedAt;
    $this.program_1 = program;
    if (0 === (seen0 & 8))
      $this.slots_1 = emptyList();
    else
      $this.slots_1 = slots;
    if (0 === (seen0 & 16))
      $this.templates_1 = emptyList();
    else
      $this.templates_1 = templates;
    if (0 === (seen0 & 32))
      $this.templateExercises_1 = emptyList();
    else
      $this.templateExercises_1 = templateExercises;
    if (0 === (seen0 & 64))
      $this.templateSets_1 = emptyList();
    else
      $this.templateSets_1 = templateSets;
    if (0 === (seen0 & 128))
      $this.exercises_1 = emptyList();
    else
      $this.exercises_1 = exercises;
    if (0 === (seen0 & 256))
      $this.edit_1 = null;
    else
      $this.edit_1 = edit;
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
    this.formatVersion_1 = formatVersion;
    this.exportedAt_1 = exportedAt;
    this.program_1 = program;
    this.slots_1 = slots;
    this.templates_1 = templates;
    this.templateExercises_1 = templateExercises;
    this.templateSets_1 = templateSets;
    this.exercises_1 = exercises;
    this.edit_1 = edit;
  }
  protoOf(ProgramDocument).toString = function () {
    return 'ProgramDocument(formatVersion=' + this.formatVersion_1 + ', exportedAt=' + this.exportedAt_1.toString() + ', program=' + this.program_1.toString() + ', slots=' + toString(this.slots_1) + ', templates=' + toString(this.templates_1) + ', templateExercises=' + toString(this.templateExercises_1) + ', templateSets=' + toString(this.templateSets_1) + ', exercises=' + toString(this.exercises_1) + ', edit=' + toString_0(this.edit_1) + ')';
  };
  protoOf(ProgramDocument).hashCode = function () {
    var result = this.formatVersion_1;
    result = imul(result, 31) + this.exportedAt_1.hashCode() | 0;
    result = imul(result, 31) + this.program_1.hashCode() | 0;
    result = imul(result, 31) + hashCode(this.slots_1) | 0;
    result = imul(result, 31) + hashCode(this.templates_1) | 0;
    result = imul(result, 31) + hashCode(this.templateExercises_1) | 0;
    result = imul(result, 31) + hashCode(this.templateSets_1) | 0;
    result = imul(result, 31) + hashCode(this.exercises_1) | 0;
    result = imul(result, 31) + (this.edit_1 == null ? 0 : this.edit_1.hashCode()) | 0;
    return result;
  };
  protoOf(ProgramDocument).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ProgramDocument))
      return false;
    if (!(this.formatVersion_1 === other.formatVersion_1))
      return false;
    if (!equalsLong(this.exportedAt_1, other.exportedAt_1))
      return false;
    if (!this.program_1.equals(other.program_1))
      return false;
    if (!equals(this.slots_1, other.slots_1))
      return false;
    if (!equals(this.templates_1, other.templates_1))
      return false;
    if (!equals(this.templateExercises_1, other.templateExercises_1))
      return false;
    if (!equals(this.templateSets_1, other.templateSets_1))
      return false;
    if (!equals(this.exercises_1, other.exercises_1))
      return false;
    if (!equals(this.edit_1, other.edit_1))
      return false;
    return true;
  };
  function $serializer_5() {
    $serializer_instance_5 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.EditMarker', this, 1);
    tmp0_serialDesc.addElement_5pzumi_k$('baseExportedAt', false);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_5).serialize_gts1x9_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 0, value.baseExportedAt_1);
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_5).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_gts1x9_k$(encoder, value instanceof EditMarker ? value : THROW_CCE());
  };
  protoOf($serializer_5).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = new Long(0, 0);
    var tmp5_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    if (tmp5_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp5_input.decodeLongElement_994anb_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp5_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp5_input.decodeLongElement_994anb_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp5_input.endStructure_1xqz0n_k$(tmp0_desc);
    return EditMarker_init_$Create$(tmp3_bitMask0, tmp4_local0, null);
  };
  protoOf($serializer_5).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_5).childSerializers_5ghqw5_k$ = function () {
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
      throwMissingFieldException(seen0, 1, $serializer_getInstance_5().descriptor_1);
    }
    $this.baseExportedAt_1 = baseExportedAt;
    return $this;
  }
  function EditMarker_init_$Create$(seen0, baseExportedAt, serializationConstructorMarker) {
    return EditMarker_init_$Init$(seen0, baseExportedAt, serializationConstructorMarker, objectCreate(protoOf(EditMarker)));
  }
  function EditMarker() {
  }
  protoOf(EditMarker).toString = function () {
    return 'EditMarker(baseExportedAt=' + this.baseExportedAt_1.toString() + ')';
  };
  protoOf(EditMarker).hashCode = function () {
    return this.baseExportedAt_1.hashCode();
  };
  protoOf(EditMarker).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof EditMarker))
      return false;
    if (!equalsLong(this.baseExportedAt_1, other.baseExportedAt_1))
      return false;
    return true;
  };
  function $serializer_6() {
    $serializer_instance_6 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.ProgramDto', this, 7);
    tmp0_serialDesc.addElement_5pzumi_k$('id', false);
    tmp0_serialDesc.addElement_5pzumi_k$('name', false);
    tmp0_serialDesc.addElement_5pzumi_k$('isActive', true);
    tmp0_serialDesc.addElement_5pzumi_k$('position', true);
    tmp0_serialDesc.addElement_5pzumi_k$('createdAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('updatedAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('deletedAt', true);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_6).serialize_1hczuu_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 0, value.id_1);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 1, value.name_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 2) ? true : !(value.isActive_1 === false)) {
      tmp1_output.encodeBooleanElement_ydht7q_k$(tmp0_desc, 2, value.isActive_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 3) ? true : !(value.position_1 === 0)) {
      tmp1_output.encodeIntElement_krhhce_k$(tmp0_desc, 3, value.position_1);
    }
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 4, value.createdAt_1);
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 5, value.updatedAt_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 6) ? true : !(value.deletedAt_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 6, LongSerializer_getInstance(), value.deletedAt_1);
    }
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_6).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_1hczuu_k$(encoder, value instanceof ProgramDto ? value : THROW_CCE());
  };
  protoOf($serializer_6).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = false;
    var tmp7_local3 = 0;
    var tmp8_local4 = new Long(0, 0);
    var tmp9_local5 = new Long(0, 0);
    var tmp10_local6 = null;
    var tmp11_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    if (tmp11_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp11_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp11_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp11_input.decodeBooleanElement_vuyhtj_k$(tmp0_desc, 2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp11_input.decodeIntElement_941u6a_k$(tmp0_desc, 3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp11_input.decodeLongElement_994anb_k$(tmp0_desc, 4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp11_input.decodeLongElement_994anb_k$(tmp0_desc, 5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp11_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 6, LongSerializer_getInstance(), tmp10_local6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp11_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp11_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp11_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp11_input.decodeBooleanElement_vuyhtj_k$(tmp0_desc, 2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp11_input.decodeIntElement_941u6a_k$(tmp0_desc, 3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp11_input.decodeLongElement_994anb_k$(tmp0_desc, 4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp11_input.decodeLongElement_994anb_k$(tmp0_desc, 5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp11_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 6, LongSerializer_getInstance(), tmp10_local6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp11_input.endStructure_1xqz0n_k$(tmp0_desc);
    return ProgramDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, null);
  };
  protoOf($serializer_6).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_6).childSerializers_5ghqw5_k$ = function () {
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), BooleanSerializer_getInstance(), IntSerializer_getInstance(), LongSerializer_getInstance(), LongSerializer_getInstance(), get_nullable(LongSerializer_getInstance())];
  };
  var $serializer_instance_6;
  function $serializer_getInstance_6() {
    if ($serializer_instance_6 == null)
      new $serializer_6();
    return $serializer_instance_6;
  }
  function ProgramDto_init_$Init$(seen0, id, name, isActive, position, createdAt, updatedAt, deletedAt, serializationConstructorMarker, $this) {
    if (!(51 === (51 & seen0))) {
      throwMissingFieldException(seen0, 51, $serializer_getInstance_6().descriptor_1);
    }
    $this.id_1 = id;
    $this.name_1 = name;
    if (0 === (seen0 & 4))
      $this.isActive_1 = false;
    else
      $this.isActive_1 = isActive;
    if (0 === (seen0 & 8))
      $this.position_1 = 0;
    else
      $this.position_1 = position;
    $this.createdAt_1 = createdAt;
    $this.updatedAt_1 = updatedAt;
    if (0 === (seen0 & 64))
      $this.deletedAt_1 = null;
    else
      $this.deletedAt_1 = deletedAt;
    return $this;
  }
  function ProgramDto_init_$Create$(seen0, id, name, isActive, position, createdAt, updatedAt, deletedAt, serializationConstructorMarker) {
    return ProgramDto_init_$Init$(seen0, id, name, isActive, position, createdAt, updatedAt, deletedAt, serializationConstructorMarker, objectCreate(protoOf(ProgramDto)));
  }
  function ProgramDto() {
  }
  protoOf(ProgramDto).toString = function () {
    return 'ProgramDto(id=' + this.id_1 + ', name=' + this.name_1 + ', isActive=' + this.isActive_1 + ', position=' + this.position_1 + ', createdAt=' + this.createdAt_1.toString() + ', updatedAt=' + this.updatedAt_1.toString() + ', deletedAt=' + toString_0(this.deletedAt_1) + ')';
  };
  protoOf(ProgramDto).hashCode = function () {
    var result = getStringHashCode(this.id_1);
    result = imul(result, 31) + getStringHashCode(this.name_1) | 0;
    result = imul(result, 31) + getBooleanHashCode(this.isActive_1) | 0;
    result = imul(result, 31) + this.position_1 | 0;
    result = imul(result, 31) + this.createdAt_1.hashCode() | 0;
    result = imul(result, 31) + this.updatedAt_1.hashCode() | 0;
    result = imul(result, 31) + (this.deletedAt_1 == null ? 0 : this.deletedAt_1.hashCode()) | 0;
    return result;
  };
  protoOf(ProgramDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ProgramDto))
      return false;
    if (!(this.id_1 === other.id_1))
      return false;
    if (!(this.name_1 === other.name_1))
      return false;
    if (!(this.isActive_1 === other.isActive_1))
      return false;
    if (!(this.position_1 === other.position_1))
      return false;
    if (!equalsLong(this.createdAt_1, other.createdAt_1))
      return false;
    if (!equalsLong(this.updatedAt_1, other.updatedAt_1))
      return false;
    if (!equals(this.deletedAt_1, other.deletedAt_1))
      return false;
    return true;
  };
  function ProgramSlotDto$Companion$$childSerializers$_anonymous__iuzt59() {
    return Companion_getInstance_8().serializer_9w0wvi_k$();
  }
  function Companion_5() {
    Companion_instance_6 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.$childSerializers_1 = [null, null, null, null, lazy(tmp_0, ProgramSlotDto$Companion$$childSerializers$_anonymous__iuzt59), null, null, null];
  }
  var Companion_instance_6;
  function Companion_getInstance_5() {
    if (Companion_instance_6 == null)
      new Companion_5();
    return Companion_instance_6;
  }
  function $serializer_7() {
    $serializer_instance_7 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.ProgramSlotDto', this, 8);
    tmp0_serialDesc.addElement_5pzumi_k$('id', false);
    tmp0_serialDesc.addElement_5pzumi_k$('programId', false);
    tmp0_serialDesc.addElement_5pzumi_k$('templateId', false);
    tmp0_serialDesc.addElement_5pzumi_k$('position', false);
    tmp0_serialDesc.addElement_5pzumi_k$('weekday', true);
    tmp0_serialDesc.addElement_5pzumi_k$('createdAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('updatedAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('deletedAt', true);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_7).serialize_cdcbhg_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp2_cached = Companion_getInstance_5().$childSerializers_1;
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 0, value.id_1);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 1, value.programId_1);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 2, value.templateId_1);
    tmp1_output.encodeIntElement_krhhce_k$(tmp0_desc, 3, value.position_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 4) ? true : !(value.weekday_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 4, tmp2_cached[4].get_value_j01efc_k$(), value.weekday_1);
    }
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 5, value.createdAt_1);
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 6, value.updatedAt_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 7) ? true : !(value.deletedAt_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 7, LongSerializer_getInstance(), value.deletedAt_1);
    }
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_7).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_cdcbhg_k$(encoder, value instanceof ProgramSlotDto ? value : THROW_CCE());
  };
  protoOf($serializer_7).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
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
    var tmp12_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp13_cached = Companion_getInstance_5().$childSerializers_1;
    if (tmp12_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp12_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp12_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp12_input.decodeStringElement_3oenpg_k$(tmp0_desc, 2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp12_input.decodeIntElement_941u6a_k$(tmp0_desc, 3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp12_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 4, tmp13_cached[4].get_value_j01efc_k$(), tmp8_local4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp12_input.decodeLongElement_994anb_k$(tmp0_desc, 5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp12_input.decodeLongElement_994anb_k$(tmp0_desc, 6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
      tmp11_local7 = tmp12_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 7, LongSerializer_getInstance(), tmp11_local7);
      tmp3_bitMask0 = tmp3_bitMask0 | 128;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp12_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp12_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp12_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp12_input.decodeStringElement_3oenpg_k$(tmp0_desc, 2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp12_input.decodeIntElement_941u6a_k$(tmp0_desc, 3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp12_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 4, tmp13_cached[4].get_value_j01efc_k$(), tmp8_local4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp12_input.decodeLongElement_994anb_k$(tmp0_desc, 5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp12_input.decodeLongElement_994anb_k$(tmp0_desc, 6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          case 7:
            tmp11_local7 = tmp12_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 7, LongSerializer_getInstance(), tmp11_local7);
            tmp3_bitMask0 = tmp3_bitMask0 | 128;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp12_input.endStructure_1xqz0n_k$(tmp0_desc);
    return ProgramSlotDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, tmp11_local7, null);
  };
  protoOf($serializer_7).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_7).childSerializers_5ghqw5_k$ = function () {
    var tmp0_cached = Companion_getInstance_5().$childSerializers_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), StringSerializer_getInstance(), IntSerializer_getInstance(), get_nullable(tmp0_cached[4].get_value_j01efc_k$()), LongSerializer_getInstance(), LongSerializer_getInstance(), get_nullable(LongSerializer_getInstance())];
  };
  var $serializer_instance_7;
  function $serializer_getInstance_7() {
    if ($serializer_instance_7 == null)
      new $serializer_7();
    return $serializer_instance_7;
  }
  function ProgramSlotDto_init_$Init$(seen0, id, programId, templateId, position, weekday, createdAt, updatedAt, deletedAt, serializationConstructorMarker, $this) {
    if (!(111 === (111 & seen0))) {
      throwMissingFieldException(seen0, 111, $serializer_getInstance_7().descriptor_1);
    }
    $this.id_1 = id;
    $this.programId_1 = programId;
    $this.templateId_1 = templateId;
    $this.position_1 = position;
    if (0 === (seen0 & 16))
      $this.weekday_1 = null;
    else
      $this.weekday_1 = weekday;
    $this.createdAt_1 = createdAt;
    $this.updatedAt_1 = updatedAt;
    if (0 === (seen0 & 128))
      $this.deletedAt_1 = null;
    else
      $this.deletedAt_1 = deletedAt;
    return $this;
  }
  function ProgramSlotDto_init_$Create$(seen0, id, programId, templateId, position, weekday, createdAt, updatedAt, deletedAt, serializationConstructorMarker) {
    return ProgramSlotDto_init_$Init$(seen0, id, programId, templateId, position, weekday, createdAt, updatedAt, deletedAt, serializationConstructorMarker, objectCreate(protoOf(ProgramSlotDto)));
  }
  function ProgramSlotDto() {
  }
  protoOf(ProgramSlotDto).toString = function () {
    return 'ProgramSlotDto(id=' + this.id_1 + ', programId=' + this.programId_1 + ', templateId=' + this.templateId_1 + ', position=' + this.position_1 + ', weekday=' + toString_0(this.weekday_1) + ', createdAt=' + this.createdAt_1.toString() + ', updatedAt=' + this.updatedAt_1.toString() + ', deletedAt=' + toString_0(this.deletedAt_1) + ')';
  };
  protoOf(ProgramSlotDto).hashCode = function () {
    var result = getStringHashCode(this.id_1);
    result = imul(result, 31) + getStringHashCode(this.programId_1) | 0;
    result = imul(result, 31) + getStringHashCode(this.templateId_1) | 0;
    result = imul(result, 31) + this.position_1 | 0;
    result = imul(result, 31) + (this.weekday_1 == null ? 0 : this.weekday_1.hashCode()) | 0;
    result = imul(result, 31) + this.createdAt_1.hashCode() | 0;
    result = imul(result, 31) + this.updatedAt_1.hashCode() | 0;
    result = imul(result, 31) + (this.deletedAt_1 == null ? 0 : this.deletedAt_1.hashCode()) | 0;
    return result;
  };
  protoOf(ProgramSlotDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ProgramSlotDto))
      return false;
    if (!(this.id_1 === other.id_1))
      return false;
    if (!(this.programId_1 === other.programId_1))
      return false;
    if (!(this.templateId_1 === other.templateId_1))
      return false;
    if (!(this.position_1 === other.position_1))
      return false;
    if (!equals(this.weekday_1, other.weekday_1))
      return false;
    if (!equalsLong(this.createdAt_1, other.createdAt_1))
      return false;
    if (!equalsLong(this.updatedAt_1, other.updatedAt_1))
      return false;
    if (!equals(this.deletedAt_1, other.deletedAt_1))
      return false;
    return true;
  };
  function $serializer_8() {
    $serializer_instance_8 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.TemplateDto', this, 6);
    tmp0_serialDesc.addElement_5pzumi_k$('id', false);
    tmp0_serialDesc.addElement_5pzumi_k$('name', false);
    tmp0_serialDesc.addElement_5pzumi_k$('versionOf', true);
    tmp0_serialDesc.addElement_5pzumi_k$('createdAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('updatedAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('deletedAt', true);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_8).serialize_qsxyow_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 0, value.id_1);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 1, value.name_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 2) ? true : !(value.versionOf_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 2, StringSerializer_getInstance(), value.versionOf_1);
    }
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 3, value.createdAt_1);
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 4, value.updatedAt_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 5) ? true : !(value.deletedAt_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 5, LongSerializer_getInstance(), value.deletedAt_1);
    }
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_8).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_qsxyow_k$(encoder, value instanceof TemplateDto ? value : THROW_CCE());
  };
  protoOf($serializer_8).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_flag = true;
    var tmp2_index = 0;
    var tmp3_bitMask0 = 0;
    var tmp4_local0 = null;
    var tmp5_local1 = null;
    var tmp6_local2 = null;
    var tmp7_local3 = new Long(0, 0);
    var tmp8_local4 = new Long(0, 0);
    var tmp9_local5 = null;
    var tmp10_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    if (tmp10_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp10_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp10_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp10_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp10_input.decodeLongElement_994anb_k$(tmp0_desc, 3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp10_input.decodeLongElement_994anb_k$(tmp0_desc, 4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp10_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 5, LongSerializer_getInstance(), tmp9_local5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp10_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp10_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp10_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp10_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp10_input.decodeLongElement_994anb_k$(tmp0_desc, 3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp10_input.decodeLongElement_994anb_k$(tmp0_desc, 4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp10_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 5, LongSerializer_getInstance(), tmp9_local5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp10_input.endStructure_1xqz0n_k$(tmp0_desc);
    return TemplateDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, null);
  };
  protoOf($serializer_8).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_8).childSerializers_5ghqw5_k$ = function () {
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
      throwMissingFieldException(seen0, 27, $serializer_getInstance_8().descriptor_1);
    }
    $this.id_1 = id;
    $this.name_1 = name;
    if (0 === (seen0 & 4))
      $this.versionOf_1 = null;
    else
      $this.versionOf_1 = versionOf;
    $this.createdAt_1 = createdAt;
    $this.updatedAt_1 = updatedAt;
    if (0 === (seen0 & 32))
      $this.deletedAt_1 = null;
    else
      $this.deletedAt_1 = deletedAt;
    return $this;
  }
  function TemplateDto_init_$Create$(seen0, id, name, versionOf, createdAt, updatedAt, deletedAt, serializationConstructorMarker) {
    return TemplateDto_init_$Init$(seen0, id, name, versionOf, createdAt, updatedAt, deletedAt, serializationConstructorMarker, objectCreate(protoOf(TemplateDto)));
  }
  function TemplateDto() {
  }
  protoOf(TemplateDto).toString = function () {
    return 'TemplateDto(id=' + this.id_1 + ', name=' + this.name_1 + ', versionOf=' + this.versionOf_1 + ', createdAt=' + this.createdAt_1.toString() + ', updatedAt=' + this.updatedAt_1.toString() + ', deletedAt=' + toString_0(this.deletedAt_1) + ')';
  };
  protoOf(TemplateDto).hashCode = function () {
    var result = getStringHashCode(this.id_1);
    result = imul(result, 31) + getStringHashCode(this.name_1) | 0;
    result = imul(result, 31) + (this.versionOf_1 == null ? 0 : getStringHashCode(this.versionOf_1)) | 0;
    result = imul(result, 31) + this.createdAt_1.hashCode() | 0;
    result = imul(result, 31) + this.updatedAt_1.hashCode() | 0;
    result = imul(result, 31) + (this.deletedAt_1 == null ? 0 : this.deletedAt_1.hashCode()) | 0;
    return result;
  };
  protoOf(TemplateDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof TemplateDto))
      return false;
    if (!(this.id_1 === other.id_1))
      return false;
    if (!(this.name_1 === other.name_1))
      return false;
    if (!(this.versionOf_1 == other.versionOf_1))
      return false;
    if (!equalsLong(this.createdAt_1, other.createdAt_1))
      return false;
    if (!equalsLong(this.updatedAt_1, other.updatedAt_1))
      return false;
    if (!equals(this.deletedAt_1, other.deletedAt_1))
      return false;
    return true;
  };
  function $serializer_9() {
    $serializer_instance_9 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.TemplateExerciseDto', this, 11);
    tmp0_serialDesc.addElement_5pzumi_k$('id', false);
    tmp0_serialDesc.addElement_5pzumi_k$('templateId', false);
    tmp0_serialDesc.addElement_5pzumi_k$('exerciseId', false);
    tmp0_serialDesc.addElement_5pzumi_k$('position', false);
    tmp0_serialDesc.addElement_5pzumi_k$('restSeconds', true);
    tmp0_serialDesc.addElement_5pzumi_k$('techniqueNote', true);
    tmp0_serialDesc.addElement_5pzumi_k$('targetRpeHalves', true);
    tmp0_serialDesc.addElement_5pzumi_k$('supersetGroup', true);
    tmp0_serialDesc.addElement_5pzumi_k$('createdAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('updatedAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('deletedAt', true);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_9).serialize_qlz960_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 0, value.id_1);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 1, value.templateId_1);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 2, value.exerciseId_1);
    tmp1_output.encodeIntElement_krhhce_k$(tmp0_desc, 3, value.position_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 4) ? true : !(value.restSeconds_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 4, IntSerializer_getInstance(), value.restSeconds_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 5) ? true : !(value.techniqueNote_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 5, StringSerializer_getInstance(), value.techniqueNote_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 6) ? true : !(value.targetRpeHalves_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 6, IntSerializer_getInstance(), value.targetRpeHalves_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 7) ? true : !(value.supersetGroup_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 7, IntSerializer_getInstance(), value.supersetGroup_1);
    }
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 8, value.createdAt_1);
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 9, value.updatedAt_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 10) ? true : !(value.deletedAt_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 10, LongSerializer_getInstance(), value.deletedAt_1);
    }
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_9).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_qlz960_k$(encoder, value instanceof TemplateExerciseDto ? value : THROW_CCE());
  };
  protoOf($serializer_9).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
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
    var tmp15_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    if (tmp15_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp15_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp15_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp15_input.decodeStringElement_3oenpg_k$(tmp0_desc, 2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp15_input.decodeIntElement_941u6a_k$(tmp0_desc, 3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp15_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 4, IntSerializer_getInstance(), tmp8_local4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp15_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 5, StringSerializer_getInstance(), tmp9_local5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp15_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 6, IntSerializer_getInstance(), tmp10_local6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
      tmp11_local7 = tmp15_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 7, IntSerializer_getInstance(), tmp11_local7);
      tmp3_bitMask0 = tmp3_bitMask0 | 128;
      tmp12_local8 = tmp15_input.decodeLongElement_994anb_k$(tmp0_desc, 8);
      tmp3_bitMask0 = tmp3_bitMask0 | 256;
      tmp13_local9 = tmp15_input.decodeLongElement_994anb_k$(tmp0_desc, 9);
      tmp3_bitMask0 = tmp3_bitMask0 | 512;
      tmp14_local10 = tmp15_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 10, LongSerializer_getInstance(), tmp14_local10);
      tmp3_bitMask0 = tmp3_bitMask0 | 1024;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp15_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp15_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp15_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp15_input.decodeStringElement_3oenpg_k$(tmp0_desc, 2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp15_input.decodeIntElement_941u6a_k$(tmp0_desc, 3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp15_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 4, IntSerializer_getInstance(), tmp8_local4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp15_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 5, StringSerializer_getInstance(), tmp9_local5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp15_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 6, IntSerializer_getInstance(), tmp10_local6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          case 7:
            tmp11_local7 = tmp15_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 7, IntSerializer_getInstance(), tmp11_local7);
            tmp3_bitMask0 = tmp3_bitMask0 | 128;
            break;
          case 8:
            tmp12_local8 = tmp15_input.decodeLongElement_994anb_k$(tmp0_desc, 8);
            tmp3_bitMask0 = tmp3_bitMask0 | 256;
            break;
          case 9:
            tmp13_local9 = tmp15_input.decodeLongElement_994anb_k$(tmp0_desc, 9);
            tmp3_bitMask0 = tmp3_bitMask0 | 512;
            break;
          case 10:
            tmp14_local10 = tmp15_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 10, LongSerializer_getInstance(), tmp14_local10);
            tmp3_bitMask0 = tmp3_bitMask0 | 1024;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp15_input.endStructure_1xqz0n_k$(tmp0_desc);
    return TemplateExerciseDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, tmp11_local7, tmp12_local8, tmp13_local9, tmp14_local10, null);
  };
  protoOf($serializer_9).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_9).childSerializers_5ghqw5_k$ = function () {
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
      throwMissingFieldException(seen0, 783, $serializer_getInstance_9().descriptor_1);
    }
    $this.id_1 = id;
    $this.templateId_1 = templateId;
    $this.exerciseId_1 = exerciseId;
    $this.position_1 = position;
    if (0 === (seen0 & 16))
      $this.restSeconds_1 = null;
    else
      $this.restSeconds_1 = restSeconds;
    if (0 === (seen0 & 32))
      $this.techniqueNote_1 = null;
    else
      $this.techniqueNote_1 = techniqueNote;
    if (0 === (seen0 & 64))
      $this.targetRpeHalves_1 = null;
    else
      $this.targetRpeHalves_1 = targetRpeHalves;
    if (0 === (seen0 & 128))
      $this.supersetGroup_1 = null;
    else
      $this.supersetGroup_1 = supersetGroup;
    $this.createdAt_1 = createdAt;
    $this.updatedAt_1 = updatedAt;
    if (0 === (seen0 & 1024))
      $this.deletedAt_1 = null;
    else
      $this.deletedAt_1 = deletedAt;
    return $this;
  }
  function TemplateExerciseDto_init_$Create$(seen0, id, templateId, exerciseId, position, restSeconds, techniqueNote, targetRpeHalves, supersetGroup, createdAt, updatedAt, deletedAt, serializationConstructorMarker) {
    return TemplateExerciseDto_init_$Init$(seen0, id, templateId, exerciseId, position, restSeconds, techniqueNote, targetRpeHalves, supersetGroup, createdAt, updatedAt, deletedAt, serializationConstructorMarker, objectCreate(protoOf(TemplateExerciseDto)));
  }
  function TemplateExerciseDto() {
  }
  protoOf(TemplateExerciseDto).toString = function () {
    return 'TemplateExerciseDto(id=' + this.id_1 + ', templateId=' + this.templateId_1 + ', exerciseId=' + this.exerciseId_1 + ', position=' + this.position_1 + ', restSeconds=' + this.restSeconds_1 + ', techniqueNote=' + this.techniqueNote_1 + ', targetRpeHalves=' + this.targetRpeHalves_1 + ', supersetGroup=' + this.supersetGroup_1 + ', createdAt=' + this.createdAt_1.toString() + ', updatedAt=' + this.updatedAt_1.toString() + ', deletedAt=' + toString_0(this.deletedAt_1) + ')';
  };
  protoOf(TemplateExerciseDto).hashCode = function () {
    var result = getStringHashCode(this.id_1);
    result = imul(result, 31) + getStringHashCode(this.templateId_1) | 0;
    result = imul(result, 31) + getStringHashCode(this.exerciseId_1) | 0;
    result = imul(result, 31) + this.position_1 | 0;
    result = imul(result, 31) + (this.restSeconds_1 == null ? 0 : this.restSeconds_1) | 0;
    result = imul(result, 31) + (this.techniqueNote_1 == null ? 0 : getStringHashCode(this.techniqueNote_1)) | 0;
    result = imul(result, 31) + (this.targetRpeHalves_1 == null ? 0 : this.targetRpeHalves_1) | 0;
    result = imul(result, 31) + (this.supersetGroup_1 == null ? 0 : this.supersetGroup_1) | 0;
    result = imul(result, 31) + this.createdAt_1.hashCode() | 0;
    result = imul(result, 31) + this.updatedAt_1.hashCode() | 0;
    result = imul(result, 31) + (this.deletedAt_1 == null ? 0 : this.deletedAt_1.hashCode()) | 0;
    return result;
  };
  protoOf(TemplateExerciseDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof TemplateExerciseDto))
      return false;
    if (!(this.id_1 === other.id_1))
      return false;
    if (!(this.templateId_1 === other.templateId_1))
      return false;
    if (!(this.exerciseId_1 === other.exerciseId_1))
      return false;
    if (!(this.position_1 === other.position_1))
      return false;
    if (!(this.restSeconds_1 == other.restSeconds_1))
      return false;
    if (!(this.techniqueNote_1 == other.techniqueNote_1))
      return false;
    if (!(this.targetRpeHalves_1 == other.targetRpeHalves_1))
      return false;
    if (!(this.supersetGroup_1 == other.supersetGroup_1))
      return false;
    if (!equalsLong(this.createdAt_1, other.createdAt_1))
      return false;
    if (!equalsLong(this.updatedAt_1, other.updatedAt_1))
      return false;
    if (!equals(this.deletedAt_1, other.deletedAt_1))
      return false;
    return true;
  };
  function TemplateSetDto$Companion$$childSerializers$_anonymous__so0lp() {
    return Companion_getInstance_9().serializer_9w0wvi_k$();
  }
  function Companion_6() {
    Companion_instance_7 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.$childSerializers_1 = [null, null, null, lazy(tmp_0, TemplateSetDto$Companion$$childSerializers$_anonymous__so0lp), null, null, null, null, null, null, null, null, null, null];
  }
  var Companion_instance_7;
  function Companion_getInstance_6() {
    if (Companion_instance_7 == null)
      new Companion_6();
    return Companion_instance_7;
  }
  function $serializer_10() {
    $serializer_instance_10 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.TemplateSetDto', this, 14);
    tmp0_serialDesc.addElement_5pzumi_k$('id', false);
    tmp0_serialDesc.addElement_5pzumi_k$('templateExerciseId', false);
    tmp0_serialDesc.addElement_5pzumi_k$('setIndex', false);
    tmp0_serialDesc.addElement_5pzumi_k$('role', false);
    tmp0_serialDesc.addElement_5pzumi_k$('targetWeightGrams', true);
    tmp0_serialDesc.addElement_5pzumi_k$('targetAssistanceGrams', true);
    tmp0_serialDesc.addElement_5pzumi_k$('targetRepsMin', true);
    tmp0_serialDesc.addElement_5pzumi_k$('targetRepsMax', true);
    tmp0_serialDesc.addElement_5pzumi_k$('targetRepsCurrent', true);
    tmp0_serialDesc.addElement_5pzumi_k$('dropValueGrams', true);
    tmp0_serialDesc.addElement_5pzumi_k$('note', true);
    tmp0_serialDesc.addElement_5pzumi_k$('createdAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('updatedAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('deletedAt', true);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_10).serialize_bg5ncm_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp2_cached = Companion_getInstance_6().$childSerializers_1;
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 0, value.id_1);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 1, value.templateExerciseId_1);
    tmp1_output.encodeIntElement_krhhce_k$(tmp0_desc, 2, value.setIndex_1);
    tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 3, tmp2_cached[3].get_value_j01efc_k$(), value.role_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 4) ? true : !(value.targetWeightGrams_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 4, LongSerializer_getInstance(), value.targetWeightGrams_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 5) ? true : !(value.targetAssistanceGrams_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 5, LongSerializer_getInstance(), value.targetAssistanceGrams_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 6) ? true : !(value.targetRepsMin_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 6, IntSerializer_getInstance(), value.targetRepsMin_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 7) ? true : !(value.targetRepsMax_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 7, IntSerializer_getInstance(), value.targetRepsMax_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 8) ? true : !(value.targetRepsCurrent_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 8, IntSerializer_getInstance(), value.targetRepsCurrent_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 9) ? true : !(value.dropValueGrams_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 9, LongSerializer_getInstance(), value.dropValueGrams_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 10) ? true : !(value.note_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 10, StringSerializer_getInstance(), value.note_1);
    }
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 11, value.createdAt_1);
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 12, value.updatedAt_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 13) ? true : !(value.deletedAt_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 13, LongSerializer_getInstance(), value.deletedAt_1);
    }
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_10).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_bg5ncm_k$(encoder, value instanceof TemplateSetDto ? value : THROW_CCE());
  };
  protoOf($serializer_10).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
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
    var tmp18_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp19_cached = Companion_getInstance_6().$childSerializers_1;
    if (tmp18_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp18_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp18_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp18_input.decodeIntElement_941u6a_k$(tmp0_desc, 2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp18_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 3, tmp19_cached[3].get_value_j01efc_k$(), tmp7_local3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 4, LongSerializer_getInstance(), tmp8_local4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 5, LongSerializer_getInstance(), tmp9_local5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 6, IntSerializer_getInstance(), tmp10_local6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
      tmp11_local7 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 7, IntSerializer_getInstance(), tmp11_local7);
      tmp3_bitMask0 = tmp3_bitMask0 | 128;
      tmp12_local8 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 8, IntSerializer_getInstance(), tmp12_local8);
      tmp3_bitMask0 = tmp3_bitMask0 | 256;
      tmp13_local9 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 9, LongSerializer_getInstance(), tmp13_local9);
      tmp3_bitMask0 = tmp3_bitMask0 | 512;
      tmp14_local10 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 10, StringSerializer_getInstance(), tmp14_local10);
      tmp3_bitMask0 = tmp3_bitMask0 | 1024;
      tmp15_local11 = tmp18_input.decodeLongElement_994anb_k$(tmp0_desc, 11);
      tmp3_bitMask0 = tmp3_bitMask0 | 2048;
      tmp16_local12 = tmp18_input.decodeLongElement_994anb_k$(tmp0_desc, 12);
      tmp3_bitMask0 = tmp3_bitMask0 | 4096;
      tmp17_local13 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 13, LongSerializer_getInstance(), tmp17_local13);
      tmp3_bitMask0 = tmp3_bitMask0 | 8192;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp18_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp18_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp18_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp18_input.decodeIntElement_941u6a_k$(tmp0_desc, 2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp18_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 3, tmp19_cached[3].get_value_j01efc_k$(), tmp7_local3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 4, LongSerializer_getInstance(), tmp8_local4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 5, LongSerializer_getInstance(), tmp9_local5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 6, IntSerializer_getInstance(), tmp10_local6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          case 7:
            tmp11_local7 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 7, IntSerializer_getInstance(), tmp11_local7);
            tmp3_bitMask0 = tmp3_bitMask0 | 128;
            break;
          case 8:
            tmp12_local8 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 8, IntSerializer_getInstance(), tmp12_local8);
            tmp3_bitMask0 = tmp3_bitMask0 | 256;
            break;
          case 9:
            tmp13_local9 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 9, LongSerializer_getInstance(), tmp13_local9);
            tmp3_bitMask0 = tmp3_bitMask0 | 512;
            break;
          case 10:
            tmp14_local10 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 10, StringSerializer_getInstance(), tmp14_local10);
            tmp3_bitMask0 = tmp3_bitMask0 | 1024;
            break;
          case 11:
            tmp15_local11 = tmp18_input.decodeLongElement_994anb_k$(tmp0_desc, 11);
            tmp3_bitMask0 = tmp3_bitMask0 | 2048;
            break;
          case 12:
            tmp16_local12 = tmp18_input.decodeLongElement_994anb_k$(tmp0_desc, 12);
            tmp3_bitMask0 = tmp3_bitMask0 | 4096;
            break;
          case 13:
            tmp17_local13 = tmp18_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 13, LongSerializer_getInstance(), tmp17_local13);
            tmp3_bitMask0 = tmp3_bitMask0 | 8192;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp18_input.endStructure_1xqz0n_k$(tmp0_desc);
    return TemplateSetDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, tmp11_local7, tmp12_local8, tmp13_local9, tmp14_local10, tmp15_local11, tmp16_local12, tmp17_local13, null);
  };
  protoOf($serializer_10).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_10).childSerializers_5ghqw5_k$ = function () {
    var tmp0_cached = Companion_getInstance_6().$childSerializers_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), IntSerializer_getInstance(), tmp0_cached[3].get_value_j01efc_k$(), get_nullable(LongSerializer_getInstance()), get_nullable(LongSerializer_getInstance()), get_nullable(IntSerializer_getInstance()), get_nullable(IntSerializer_getInstance()), get_nullable(IntSerializer_getInstance()), get_nullable(LongSerializer_getInstance()), get_nullable(StringSerializer_getInstance()), LongSerializer_getInstance(), LongSerializer_getInstance(), get_nullable(LongSerializer_getInstance())];
  };
  var $serializer_instance_10;
  function $serializer_getInstance_10() {
    if ($serializer_instance_10 == null)
      new $serializer_10();
    return $serializer_instance_10;
  }
  function TemplateSetDto_init_$Init$(seen0, id, templateExerciseId, setIndex, role, targetWeightGrams, targetAssistanceGrams, targetRepsMin, targetRepsMax, targetRepsCurrent, dropValueGrams, note, createdAt, updatedAt, deletedAt, serializationConstructorMarker, $this) {
    if (!(6159 === (6159 & seen0))) {
      throwMissingFieldException(seen0, 6159, $serializer_getInstance_10().descriptor_1);
    }
    $this.id_1 = id;
    $this.templateExerciseId_1 = templateExerciseId;
    $this.setIndex_1 = setIndex;
    $this.role_1 = role;
    if (0 === (seen0 & 16))
      $this.targetWeightGrams_1 = null;
    else
      $this.targetWeightGrams_1 = targetWeightGrams;
    if (0 === (seen0 & 32))
      $this.targetAssistanceGrams_1 = null;
    else
      $this.targetAssistanceGrams_1 = targetAssistanceGrams;
    if (0 === (seen0 & 64))
      $this.targetRepsMin_1 = null;
    else
      $this.targetRepsMin_1 = targetRepsMin;
    if (0 === (seen0 & 128))
      $this.targetRepsMax_1 = null;
    else
      $this.targetRepsMax_1 = targetRepsMax;
    if (0 === (seen0 & 256))
      $this.targetRepsCurrent_1 = null;
    else
      $this.targetRepsCurrent_1 = targetRepsCurrent;
    if (0 === (seen0 & 512))
      $this.dropValueGrams_1 = null;
    else
      $this.dropValueGrams_1 = dropValueGrams;
    if (0 === (seen0 & 1024))
      $this.note_1 = null;
    else
      $this.note_1 = note;
    $this.createdAt_1 = createdAt;
    $this.updatedAt_1 = updatedAt;
    if (0 === (seen0 & 8192))
      $this.deletedAt_1 = null;
    else
      $this.deletedAt_1 = deletedAt;
    return $this;
  }
  function TemplateSetDto_init_$Create$(seen0, id, templateExerciseId, setIndex, role, targetWeightGrams, targetAssistanceGrams, targetRepsMin, targetRepsMax, targetRepsCurrent, dropValueGrams, note, createdAt, updatedAt, deletedAt, serializationConstructorMarker) {
    return TemplateSetDto_init_$Init$(seen0, id, templateExerciseId, setIndex, role, targetWeightGrams, targetAssistanceGrams, targetRepsMin, targetRepsMax, targetRepsCurrent, dropValueGrams, note, createdAt, updatedAt, deletedAt, serializationConstructorMarker, objectCreate(protoOf(TemplateSetDto)));
  }
  function TemplateSetDto() {
  }
  protoOf(TemplateSetDto).toString = function () {
    return 'TemplateSetDto(id=' + this.id_1 + ', templateExerciseId=' + this.templateExerciseId_1 + ', setIndex=' + this.setIndex_1 + ', role=' + this.role_1.toString() + ', targetWeightGrams=' + toString_0(this.targetWeightGrams_1) + ', targetAssistanceGrams=' + toString_0(this.targetAssistanceGrams_1) + ', targetRepsMin=' + this.targetRepsMin_1 + ', targetRepsMax=' + this.targetRepsMax_1 + ', targetRepsCurrent=' + this.targetRepsCurrent_1 + ', dropValueGrams=' + toString_0(this.dropValueGrams_1) + ', note=' + this.note_1 + ', createdAt=' + this.createdAt_1.toString() + ', updatedAt=' + this.updatedAt_1.toString() + ', deletedAt=' + toString_0(this.deletedAt_1) + ')';
  };
  protoOf(TemplateSetDto).hashCode = function () {
    var result = getStringHashCode(this.id_1);
    result = imul(result, 31) + getStringHashCode(this.templateExerciseId_1) | 0;
    result = imul(result, 31) + this.setIndex_1 | 0;
    result = imul(result, 31) + this.role_1.hashCode() | 0;
    result = imul(result, 31) + (this.targetWeightGrams_1 == null ? 0 : this.targetWeightGrams_1.hashCode()) | 0;
    result = imul(result, 31) + (this.targetAssistanceGrams_1 == null ? 0 : this.targetAssistanceGrams_1.hashCode()) | 0;
    result = imul(result, 31) + (this.targetRepsMin_1 == null ? 0 : this.targetRepsMin_1) | 0;
    result = imul(result, 31) + (this.targetRepsMax_1 == null ? 0 : this.targetRepsMax_1) | 0;
    result = imul(result, 31) + (this.targetRepsCurrent_1 == null ? 0 : this.targetRepsCurrent_1) | 0;
    result = imul(result, 31) + (this.dropValueGrams_1 == null ? 0 : this.dropValueGrams_1.hashCode()) | 0;
    result = imul(result, 31) + (this.note_1 == null ? 0 : getStringHashCode(this.note_1)) | 0;
    result = imul(result, 31) + this.createdAt_1.hashCode() | 0;
    result = imul(result, 31) + this.updatedAt_1.hashCode() | 0;
    result = imul(result, 31) + (this.deletedAt_1 == null ? 0 : this.deletedAt_1.hashCode()) | 0;
    return result;
  };
  protoOf(TemplateSetDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof TemplateSetDto))
      return false;
    if (!(this.id_1 === other.id_1))
      return false;
    if (!(this.templateExerciseId_1 === other.templateExerciseId_1))
      return false;
    if (!(this.setIndex_1 === other.setIndex_1))
      return false;
    if (!this.role_1.equals(other.role_1))
      return false;
    if (!equals(this.targetWeightGrams_1, other.targetWeightGrams_1))
      return false;
    if (!equals(this.targetAssistanceGrams_1, other.targetAssistanceGrams_1))
      return false;
    if (!(this.targetRepsMin_1 == other.targetRepsMin_1))
      return false;
    if (!(this.targetRepsMax_1 == other.targetRepsMax_1))
      return false;
    if (!(this.targetRepsCurrent_1 == other.targetRepsCurrent_1))
      return false;
    if (!equals(this.dropValueGrams_1, other.dropValueGrams_1))
      return false;
    if (!(this.note_1 == other.note_1))
      return false;
    if (!equalsLong(this.createdAt_1, other.createdAt_1))
      return false;
    if (!equalsLong(this.updatedAt_1, other.updatedAt_1))
      return false;
    if (!equals(this.deletedAt_1, other.deletedAt_1))
      return false;
    return true;
  };
  function ExerciseDto$Companion$$childSerializers$_anonymous__falojn() {
    return new ArrayListSerializer(StringSerializer_getInstance());
  }
  function Companion_7() {
    Companion_instance_8 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    tmp.$childSerializers_1 = [null, null, null, lazy(tmp_0, ExerciseDto$Companion$$childSerializers$_anonymous__falojn), null, null, null, null, null, null, null, null, null, null, null];
  }
  var Companion_instance_8;
  function Companion_getInstance_7() {
    if (Companion_instance_8 == null)
      new Companion_7();
    return Companion_instance_8;
  }
  function $serializer_11() {
    $serializer_instance_11 = this;
    var tmp0_serialDesc = new PluginGeneratedSerialDescriptor('com.example.androidapp.shared.ExerciseDto', this, 15);
    tmp0_serialDesc.addElement_5pzumi_k$('id', false);
    tmp0_serialDesc.addElement_5pzumi_k$('name', false);
    tmp0_serialDesc.addElement_5pzumi_k$('primaryMuscle', false);
    tmp0_serialDesc.addElement_5pzumi_k$('secondaryMuscles', false);
    tmp0_serialDesc.addElement_5pzumi_k$('equipment', false);
    tmp0_serialDesc.addElement_5pzumi_k$('movementPattern', false);
    tmp0_serialDesc.addElement_5pzumi_k$('createdAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('updatedAt', false);
    tmp0_serialDesc.addElement_5pzumi_k$('deletedAt', true);
    tmp0_serialDesc.addElement_5pzumi_k$('restSeconds', true);
    tmp0_serialDesc.addElement_5pzumi_k$('techniqueNote', true);
    tmp0_serialDesc.addElement_5pzumi_k$('weightUnit', true);
    tmp0_serialDesc.addElement_5pzumi_k$('stepGrams', true);
    tmp0_serialDesc.addElement_5pzumi_k$('parentId', true);
    tmp0_serialDesc.addElement_5pzumi_k$('rowKind', true);
    this.descriptor_1 = tmp0_serialDesc;
  }
  protoOf($serializer_11).serialize_viznsy_k$ = function (encoder, value) {
    var tmp0_desc = this.descriptor_1;
    var tmp1_output = encoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp2_cached = Companion_getInstance_7().$childSerializers_1;
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 0, value.id_1);
    tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 1, value.name_1);
    tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 2, StringSerializer_getInstance(), value.primaryMuscle_1);
    tmp1_output.encodeSerializableElement_isqxcl_k$(tmp0_desc, 3, tmp2_cached[3].get_value_j01efc_k$(), value.secondaryMuscles_1);
    tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 4, StringSerializer_getInstance(), value.equipment_1);
    tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 5, StringSerializer_getInstance(), value.movementPattern_1);
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 6, value.createdAt_1);
    tmp1_output.encodeLongElement_ovnxxh_k$(tmp0_desc, 7, value.updatedAt_1);
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 8) ? true : !(value.deletedAt_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 8, LongSerializer_getInstance(), value.deletedAt_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 9) ? true : !(value.restSeconds_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 9, IntSerializer_getInstance(), value.restSeconds_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 10) ? true : !(value.techniqueNote_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 10, StringSerializer_getInstance(), value.techniqueNote_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 11) ? true : !(value.weightUnit_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 11, StringSerializer_getInstance(), value.weightUnit_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 12) ? true : !(value.stepGrams_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 12, LongSerializer_getInstance(), value.stepGrams_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 13) ? true : !(value.parentId_1 == null)) {
      tmp1_output.encodeNullableSerializableElement_5lquiv_k$(tmp0_desc, 13, StringSerializer_getInstance(), value.parentId_1);
    }
    if (tmp1_output.shouldEncodeElementDefault_x8eyid_k$(tmp0_desc, 14) ? true : !(value.rowKind_1 === 'MOVEMENT')) {
      tmp1_output.encodeStringElement_1n5wu2_k$(tmp0_desc, 14, value.rowKind_1);
    }
    tmp1_output.endStructure_1xqz0n_k$(tmp0_desc);
  };
  protoOf($serializer_11).serialize_5ase3y_k$ = function (encoder, value) {
    return this.serialize_viznsy_k$(encoder, value instanceof ExerciseDto ? value : THROW_CCE());
  };
  protoOf($serializer_11).deserialize_sy6x50_k$ = function (decoder) {
    var tmp0_desc = this.descriptor_1;
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
    var tmp19_input = decoder.beginStructure_yljocp_k$(tmp0_desc);
    var tmp20_cached = Companion_getInstance_7().$childSerializers_1;
    if (tmp19_input.decodeSequentially_xlblqy_k$()) {
      tmp4_local0 = tmp19_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
      tmp3_bitMask0 = tmp3_bitMask0 | 1;
      tmp5_local1 = tmp19_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
      tmp3_bitMask0 = tmp3_bitMask0 | 2;
      tmp6_local2 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
      tmp3_bitMask0 = tmp3_bitMask0 | 4;
      tmp7_local3 = tmp19_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 3, tmp20_cached[3].get_value_j01efc_k$(), tmp7_local3);
      tmp3_bitMask0 = tmp3_bitMask0 | 8;
      tmp8_local4 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 4, StringSerializer_getInstance(), tmp8_local4);
      tmp3_bitMask0 = tmp3_bitMask0 | 16;
      tmp9_local5 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 5, StringSerializer_getInstance(), tmp9_local5);
      tmp3_bitMask0 = tmp3_bitMask0 | 32;
      tmp10_local6 = tmp19_input.decodeLongElement_994anb_k$(tmp0_desc, 6);
      tmp3_bitMask0 = tmp3_bitMask0 | 64;
      tmp11_local7 = tmp19_input.decodeLongElement_994anb_k$(tmp0_desc, 7);
      tmp3_bitMask0 = tmp3_bitMask0 | 128;
      tmp12_local8 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 8, LongSerializer_getInstance(), tmp12_local8);
      tmp3_bitMask0 = tmp3_bitMask0 | 256;
      tmp13_local9 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 9, IntSerializer_getInstance(), tmp13_local9);
      tmp3_bitMask0 = tmp3_bitMask0 | 512;
      tmp14_local10 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 10, StringSerializer_getInstance(), tmp14_local10);
      tmp3_bitMask0 = tmp3_bitMask0 | 1024;
      tmp15_local11 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 11, StringSerializer_getInstance(), tmp15_local11);
      tmp3_bitMask0 = tmp3_bitMask0 | 2048;
      tmp16_local12 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 12, LongSerializer_getInstance(), tmp16_local12);
      tmp3_bitMask0 = tmp3_bitMask0 | 4096;
      tmp17_local13 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 13, StringSerializer_getInstance(), tmp17_local13);
      tmp3_bitMask0 = tmp3_bitMask0 | 8192;
      tmp18_local14 = tmp19_input.decodeStringElement_3oenpg_k$(tmp0_desc, 14);
      tmp3_bitMask0 = tmp3_bitMask0 | 16384;
    } else
      while (tmp1_flag) {
        tmp2_index = tmp19_input.decodeElementIndex_bstkhp_k$(tmp0_desc);
        switch (tmp2_index) {
          case -1:
            tmp1_flag = false;
            break;
          case 0:
            tmp4_local0 = tmp19_input.decodeStringElement_3oenpg_k$(tmp0_desc, 0);
            tmp3_bitMask0 = tmp3_bitMask0 | 1;
            break;
          case 1:
            tmp5_local1 = tmp19_input.decodeStringElement_3oenpg_k$(tmp0_desc, 1);
            tmp3_bitMask0 = tmp3_bitMask0 | 2;
            break;
          case 2:
            tmp6_local2 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 2, StringSerializer_getInstance(), tmp6_local2);
            tmp3_bitMask0 = tmp3_bitMask0 | 4;
            break;
          case 3:
            tmp7_local3 = tmp19_input.decodeSerializableElement_uahnnv_k$(tmp0_desc, 3, tmp20_cached[3].get_value_j01efc_k$(), tmp7_local3);
            tmp3_bitMask0 = tmp3_bitMask0 | 8;
            break;
          case 4:
            tmp8_local4 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 4, StringSerializer_getInstance(), tmp8_local4);
            tmp3_bitMask0 = tmp3_bitMask0 | 16;
            break;
          case 5:
            tmp9_local5 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 5, StringSerializer_getInstance(), tmp9_local5);
            tmp3_bitMask0 = tmp3_bitMask0 | 32;
            break;
          case 6:
            tmp10_local6 = tmp19_input.decodeLongElement_994anb_k$(tmp0_desc, 6);
            tmp3_bitMask0 = tmp3_bitMask0 | 64;
            break;
          case 7:
            tmp11_local7 = tmp19_input.decodeLongElement_994anb_k$(tmp0_desc, 7);
            tmp3_bitMask0 = tmp3_bitMask0 | 128;
            break;
          case 8:
            tmp12_local8 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 8, LongSerializer_getInstance(), tmp12_local8);
            tmp3_bitMask0 = tmp3_bitMask0 | 256;
            break;
          case 9:
            tmp13_local9 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 9, IntSerializer_getInstance(), tmp13_local9);
            tmp3_bitMask0 = tmp3_bitMask0 | 512;
            break;
          case 10:
            tmp14_local10 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 10, StringSerializer_getInstance(), tmp14_local10);
            tmp3_bitMask0 = tmp3_bitMask0 | 1024;
            break;
          case 11:
            tmp15_local11 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 11, StringSerializer_getInstance(), tmp15_local11);
            tmp3_bitMask0 = tmp3_bitMask0 | 2048;
            break;
          case 12:
            tmp16_local12 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 12, LongSerializer_getInstance(), tmp16_local12);
            tmp3_bitMask0 = tmp3_bitMask0 | 4096;
            break;
          case 13:
            tmp17_local13 = tmp19_input.decodeNullableSerializableElement_k2y6ab_k$(tmp0_desc, 13, StringSerializer_getInstance(), tmp17_local13);
            tmp3_bitMask0 = tmp3_bitMask0 | 8192;
            break;
          case 14:
            tmp18_local14 = tmp19_input.decodeStringElement_3oenpg_k$(tmp0_desc, 14);
            tmp3_bitMask0 = tmp3_bitMask0 | 16384;
            break;
          default:
            throw UnknownFieldException_init_$Create$(tmp2_index);
        }
      }
    tmp19_input.endStructure_1xqz0n_k$(tmp0_desc);
    return ExerciseDto_init_$Create$(tmp3_bitMask0, tmp4_local0, tmp5_local1, tmp6_local2, tmp7_local3, tmp8_local4, tmp9_local5, tmp10_local6, tmp11_local7, tmp12_local8, tmp13_local9, tmp14_local10, tmp15_local11, tmp16_local12, tmp17_local13, tmp18_local14, null);
  };
  protoOf($serializer_11).get_descriptor_wjt6a0_k$ = function () {
    return this.descriptor_1;
  };
  protoOf($serializer_11).childSerializers_5ghqw5_k$ = function () {
    var tmp0_cached = Companion_getInstance_7().$childSerializers_1;
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    return [StringSerializer_getInstance(), StringSerializer_getInstance(), get_nullable(StringSerializer_getInstance()), tmp0_cached[3].get_value_j01efc_k$(), get_nullable(StringSerializer_getInstance()), get_nullable(StringSerializer_getInstance()), LongSerializer_getInstance(), LongSerializer_getInstance(), get_nullable(LongSerializer_getInstance()), get_nullable(IntSerializer_getInstance()), get_nullable(StringSerializer_getInstance()), get_nullable(StringSerializer_getInstance()), get_nullable(LongSerializer_getInstance()), get_nullable(StringSerializer_getInstance()), StringSerializer_getInstance()];
  };
  var $serializer_instance_11;
  function $serializer_getInstance_11() {
    if ($serializer_instance_11 == null)
      new $serializer_11();
    return $serializer_instance_11;
  }
  function ExerciseDto_init_$Init$(seen0, id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, createdAt, updatedAt, deletedAt, restSeconds, techniqueNote, weightUnit, stepGrams, parentId, rowKind, serializationConstructorMarker, $this) {
    if (!(255 === (255 & seen0))) {
      throwMissingFieldException(seen0, 255, $serializer_getInstance_11().descriptor_1);
    }
    $this.id_1 = id;
    $this.name_1 = name;
    $this.primaryMuscle_1 = primaryMuscle;
    $this.secondaryMuscles_1 = secondaryMuscles;
    $this.equipment_1 = equipment;
    $this.movementPattern_1 = movementPattern;
    $this.createdAt_1 = createdAt;
    $this.updatedAt_1 = updatedAt;
    if (0 === (seen0 & 256))
      $this.deletedAt_1 = null;
    else
      $this.deletedAt_1 = deletedAt;
    if (0 === (seen0 & 512))
      $this.restSeconds_1 = null;
    else
      $this.restSeconds_1 = restSeconds;
    if (0 === (seen0 & 1024))
      $this.techniqueNote_1 = null;
    else
      $this.techniqueNote_1 = techniqueNote;
    if (0 === (seen0 & 2048))
      $this.weightUnit_1 = null;
    else
      $this.weightUnit_1 = weightUnit;
    if (0 === (seen0 & 4096))
      $this.stepGrams_1 = null;
    else
      $this.stepGrams_1 = stepGrams;
    if (0 === (seen0 & 8192))
      $this.parentId_1 = null;
    else
      $this.parentId_1 = parentId;
    if (0 === (seen0 & 16384))
      $this.rowKind_1 = 'MOVEMENT';
    else
      $this.rowKind_1 = rowKind;
    return $this;
  }
  function ExerciseDto_init_$Create$(seen0, id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, createdAt, updatedAt, deletedAt, restSeconds, techniqueNote, weightUnit, stepGrams, parentId, rowKind, serializationConstructorMarker) {
    return ExerciseDto_init_$Init$(seen0, id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, createdAt, updatedAt, deletedAt, restSeconds, techniqueNote, weightUnit, stepGrams, parentId, rowKind, serializationConstructorMarker, objectCreate(protoOf(ExerciseDto)));
  }
  function ExerciseDto() {
  }
  protoOf(ExerciseDto).toString = function () {
    return 'ExerciseDto(id=' + this.id_1 + ', name=' + this.name_1 + ', primaryMuscle=' + this.primaryMuscle_1 + ', secondaryMuscles=' + toString(this.secondaryMuscles_1) + ', equipment=' + this.equipment_1 + ', movementPattern=' + this.movementPattern_1 + ', createdAt=' + this.createdAt_1.toString() + ', updatedAt=' + this.updatedAt_1.toString() + ', deletedAt=' + toString_0(this.deletedAt_1) + ', restSeconds=' + this.restSeconds_1 + ', techniqueNote=' + this.techniqueNote_1 + ', weightUnit=' + this.weightUnit_1 + ', stepGrams=' + toString_0(this.stepGrams_1) + ', parentId=' + this.parentId_1 + ', rowKind=' + this.rowKind_1 + ')';
  };
  protoOf(ExerciseDto).hashCode = function () {
    var result = getStringHashCode(this.id_1);
    result = imul(result, 31) + getStringHashCode(this.name_1) | 0;
    result = imul(result, 31) + (this.primaryMuscle_1 == null ? 0 : getStringHashCode(this.primaryMuscle_1)) | 0;
    result = imul(result, 31) + hashCode(this.secondaryMuscles_1) | 0;
    result = imul(result, 31) + (this.equipment_1 == null ? 0 : getStringHashCode(this.equipment_1)) | 0;
    result = imul(result, 31) + (this.movementPattern_1 == null ? 0 : getStringHashCode(this.movementPattern_1)) | 0;
    result = imul(result, 31) + this.createdAt_1.hashCode() | 0;
    result = imul(result, 31) + this.updatedAt_1.hashCode() | 0;
    result = imul(result, 31) + (this.deletedAt_1 == null ? 0 : this.deletedAt_1.hashCode()) | 0;
    result = imul(result, 31) + (this.restSeconds_1 == null ? 0 : this.restSeconds_1) | 0;
    result = imul(result, 31) + (this.techniqueNote_1 == null ? 0 : getStringHashCode(this.techniqueNote_1)) | 0;
    result = imul(result, 31) + (this.weightUnit_1 == null ? 0 : getStringHashCode(this.weightUnit_1)) | 0;
    result = imul(result, 31) + (this.stepGrams_1 == null ? 0 : this.stepGrams_1.hashCode()) | 0;
    result = imul(result, 31) + (this.parentId_1 == null ? 0 : getStringHashCode(this.parentId_1)) | 0;
    result = imul(result, 31) + getStringHashCode(this.rowKind_1) | 0;
    return result;
  };
  protoOf(ExerciseDto).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof ExerciseDto))
      return false;
    if (!(this.id_1 === other.id_1))
      return false;
    if (!(this.name_1 === other.name_1))
      return false;
    if (!(this.primaryMuscle_1 == other.primaryMuscle_1))
      return false;
    if (!equals(this.secondaryMuscles_1, other.secondaryMuscles_1))
      return false;
    if (!(this.equipment_1 == other.equipment_1))
      return false;
    if (!(this.movementPattern_1 == other.movementPattern_1))
      return false;
    if (!equalsLong(this.createdAt_1, other.createdAt_1))
      return false;
    if (!equalsLong(this.updatedAt_1, other.updatedAt_1))
      return false;
    if (!equals(this.deletedAt_1, other.deletedAt_1))
      return false;
    if (!(this.restSeconds_1 == other.restSeconds_1))
      return false;
    if (!(this.techniqueNote_1 == other.techniqueNote_1))
      return false;
    if (!(this.weightUnit_1 == other.weightUnit_1))
      return false;
    if (!equals(this.stepGrams_1, other.stepGrams_1))
      return false;
    if (!(this.parentId_1 == other.parentId_1))
      return false;
    if (!(this.rowKind_1 === other.rowKind_1))
      return false;
    return true;
  };
  function _get_$cachedSerializer__te6jhj($this) {
    return $this.$cachedSerializer$delegate_1.get_value_j01efc_k$();
  }
  function Weekday$Companion$_anonymous__wjgbbt() {
    return createSimpleEnumSerializer('com.example.androidapp.shared.Weekday', values());
  }
  var static_init_called_0;
  function static_init_0() {
    if (static_init_called_0)
      return Unit_instance;
    static_init_called_0 = true;
    Weekday_MONDAY_instance = new Weekday('MONDAY', 0);
    Weekday_TUESDAY_instance = new Weekday('TUESDAY', 1);
    Weekday_WEDNESDAY_instance = new Weekday('WEDNESDAY', 2);
    Weekday_THURSDAY_instance = new Weekday('THURSDAY', 3);
    Weekday_FRIDAY_instance = new Weekday('FRIDAY', 4);
    Weekday_SATURDAY_instance = new Weekday('SATURDAY', 5);
    Weekday_SUNDAY_instance = new Weekday('SUNDAY', 6);
    if (Companion_instance_9 == null) {
      Companion_instance;
      new Companion_8();
    }
  }
  var Weekday_MONDAY_instance;
  var Weekday_TUESDAY_instance;
  var Weekday_WEDNESDAY_instance;
  var Weekday_THURSDAY_instance;
  var Weekday_FRIDAY_instance;
  var Weekday_SATURDAY_instance;
  var Weekday_SUNDAY_instance;
  function values() {
    static_init_0();
    return [Weekday_MONDAY_getInstance(), Weekday_TUESDAY_getInstance(), Weekday_WEDNESDAY_getInstance(), Weekday_THURSDAY_getInstance(), Weekday_FRIDAY_getInstance(), Weekday_SATURDAY_getInstance(), Weekday_SUNDAY_getInstance()];
  }
  function Companion_8() {
    Companion_instance_9 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp.$cachedSerializer$delegate_1 = lazy(tmp_0, Weekday$Companion$_anonymous__wjgbbt);
  }
  protoOf(Companion_8).serializer_9w0wvi_k$ = function () {
    return _get_$cachedSerializer__te6jhj(this);
  };
  protoOf(Companion_8).serializer_nv39qc_k$ = function (typeParamsSerializers) {
    return this.serializer_9w0wvi_k$();
  };
  var Companion_instance_9;
  function Companion_getInstance_8() {
    static_init_0();
    return Companion_instance_9;
  }
  function Weekday(name, ordinal) {
    Enum.call(this, name, ordinal);
  }
  function _get_$cachedSerializer__te6jhj_0($this) {
    return $this.$cachedSerializer$delegate_1.get_value_j01efc_k$();
  }
  function ProgramSetRole$Companion$_anonymous__csnl4r() {
    return createSimpleEnumSerializer('com.example.androidapp.shared.ProgramSetRole', values_0());
  }
  var static_init_called_1;
  function static_init_1() {
    if (static_init_called_1)
      return Unit_instance;
    static_init_called_1 = true;
    ProgramSetRole_NORMAL_instance = new ProgramSetRole('NORMAL', 0);
    ProgramSetRole_WARMUP_instance = new ProgramSetRole('WARMUP', 1);
    ProgramSetRole_TOP_SET_instance = new ProgramSetRole('TOP_SET', 2);
    ProgramSetRole_DROP_instance = new ProgramSetRole('DROP', 3);
    ProgramSetRole_CLUSTER_instance = new ProgramSetRole('CLUSTER', 4);
    if (Companion_instance_10 == null) {
      Companion_instance;
      new Companion_9();
    }
  }
  var ProgramSetRole_NORMAL_instance;
  var ProgramSetRole_WARMUP_instance;
  var ProgramSetRole_TOP_SET_instance;
  var ProgramSetRole_DROP_instance;
  var ProgramSetRole_CLUSTER_instance;
  function values_0() {
    static_init_1();
    return [ProgramSetRole_NORMAL_getInstance(), ProgramSetRole_WARMUP_getInstance(), ProgramSetRole_TOP_SET_getInstance(), ProgramSetRole_DROP_getInstance(), ProgramSetRole_CLUSTER_getInstance()];
  }
  function Companion_9() {
    Companion_instance_10 = this;
    var tmp = this;
    var tmp_0 = LazyThreadSafetyMode_PUBLICATION_getInstance();
    tmp.$cachedSerializer$delegate_1 = lazy(tmp_0, ProgramSetRole$Companion$_anonymous__csnl4r);
  }
  protoOf(Companion_9).serializer_9w0wvi_k$ = function () {
    return _get_$cachedSerializer__te6jhj_0(this);
  };
  protoOf(Companion_9).serializer_nv39qc_k$ = function (typeParamsSerializers) {
    return this.serializer_9w0wvi_k$();
  };
  var Companion_instance_10;
  function Companion_getInstance_9() {
    static_init_1();
    return Companion_instance_10;
  }
  function ProgramSetRole(name, ordinal) {
    Enum.call(this, name, ordinal);
  }
  protoOf(ProgramSetRole).get_isRung_ew5llb_k$ = function () {
    return this.equals(ProgramSetRole_DROP_getInstance()) || this.equals(ProgramSetRole_CLUSTER_getInstance());
  };
  function Weekday_MONDAY_getInstance() {
    static_init_0();
    return Weekday_MONDAY_instance;
  }
  function Weekday_TUESDAY_getInstance() {
    static_init_0();
    return Weekday_TUESDAY_instance;
  }
  function Weekday_WEDNESDAY_getInstance() {
    static_init_0();
    return Weekday_WEDNESDAY_instance;
  }
  function Weekday_THURSDAY_getInstance() {
    static_init_0();
    return Weekday_THURSDAY_instance;
  }
  function Weekday_FRIDAY_getInstance() {
    static_init_0();
    return Weekday_FRIDAY_instance;
  }
  function Weekday_SATURDAY_getInstance() {
    static_init_0();
    return Weekday_SATURDAY_instance;
  }
  function Weekday_SUNDAY_getInstance() {
    static_init_0();
    return Weekday_SUNDAY_instance;
  }
  function ProgramSetRole_NORMAL_getInstance() {
    static_init_1();
    return ProgramSetRole_NORMAL_instance;
  }
  function ProgramSetRole_WARMUP_getInstance() {
    static_init_1();
    return ProgramSetRole_WARMUP_instance;
  }
  function ProgramSetRole_TOP_SET_getInstance() {
    static_init_1();
    return ProgramSetRole_TOP_SET_instance;
  }
  function ProgramSetRole_DROP_getInstance() {
    static_init_1();
    return ProgramSetRole_DROP_instance;
  }
  function ProgramSetRole_CLUSTER_getInstance() {
    static_init_1();
    return ProgramSetRole_CLUSTER_instance;
  }
  var static_init_called_2;
  function static_init_2() {
    if (static_init_called_2)
      return Unit_instance;
    static_init_called_2 = true;
    ChangeKind_PROGRAM_RENAMED_instance = new ChangeKind('PROGRAM_RENAMED', 0);
    ChangeKind_SLOT_ADDED_instance = new ChangeKind('SLOT_ADDED', 1);
    ChangeKind_SLOT_UPDATED_instance = new ChangeKind('SLOT_UPDATED', 2);
    ChangeKind_SLOT_REMOVED_instance = new ChangeKind('SLOT_REMOVED', 3);
    ChangeKind_TEMPLATE_ADDED_instance = new ChangeKind('TEMPLATE_ADDED', 4);
    ChangeKind_TEMPLATE_UPDATED_instance = new ChangeKind('TEMPLATE_UPDATED', 5);
    ChangeKind_TEMPLATE_EXERCISE_ADDED_instance = new ChangeKind('TEMPLATE_EXERCISE_ADDED', 6);
    ChangeKind_TEMPLATE_EXERCISE_UPDATED_instance = new ChangeKind('TEMPLATE_EXERCISE_UPDATED', 7);
    ChangeKind_TEMPLATE_EXERCISE_REMOVED_instance = new ChangeKind('TEMPLATE_EXERCISE_REMOVED', 8);
    ChangeKind_SET_ADDED_instance = new ChangeKind('SET_ADDED', 9);
    ChangeKind_SET_UPDATED_instance = new ChangeKind('SET_UPDATED', 10);
    ChangeKind_SET_REMOVED_instance = new ChangeKind('SET_REMOVED', 11);
    ChangeKind_EXERCISE_ADDED_instance = new ChangeKind('EXERCISE_ADDED', 12);
  }
  var ChangeKind_PROGRAM_RENAMED_instance;
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
  function Change(kind, id, name) {
    name = name === VOID ? null : name;
    this.kind_1 = kind;
    this.id_1 = id;
    this.name_1 = name;
  }
  protoOf(Change).toString = function () {
    return 'Change(kind=' + this.kind_1.toString() + ', id=' + this.id_1 + ', name=' + this.name_1 + ')';
  };
  protoOf(Change).hashCode = function () {
    var result = this.kind_1.hashCode();
    result = imul(result, 31) + getStringHashCode(this.id_1) | 0;
    result = imul(result, 31) + (this.name_1 == null ? 0 : getStringHashCode(this.name_1)) | 0;
    return result;
  };
  protoOf(Change).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof Change))
      return false;
    if (!this.kind_1.equals(other.kind_1))
      return false;
    if (!(this.id_1 === other.id_1))
      return false;
    if (!(this.name_1 == other.name_1))
      return false;
    return true;
  };
  var static_init_called_3;
  function static_init_3() {
    if (static_init_called_3)
      return Unit_instance;
    static_init_called_3 = true;
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
    this.code_1 = code;
    this.id_1 = id;
    this.name_1 = name;
  }
  protoOf(PlanWarning).toString = function () {
    return 'PlanWarning(code=' + this.code_1.toString() + ', id=' + this.id_1 + ', name=' + this.name_1 + ')';
  };
  protoOf(PlanWarning).hashCode = function () {
    var result = this.code_1.hashCode();
    result = imul(result, 31) + (this.id_1 == null ? 0 : getStringHashCode(this.id_1)) | 0;
    result = imul(result, 31) + (this.name_1 == null ? 0 : getStringHashCode(this.name_1)) | 0;
    return result;
  };
  protoOf(PlanWarning).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof PlanWarning))
      return false;
    if (!this.code_1.equals(other.code_1))
      return false;
    if (!(this.id_1 == other.id_1))
      return false;
    if (!(this.name_1 == other.name_1))
      return false;
    return true;
  };
  function Ready(changes, warnings) {
    this.changes_1 = changes;
    this.warnings_1 = warnings;
  }
  protoOf(Ready).toString = function () {
    return 'Ready(changes=' + toString(this.changes_1) + ', warnings=' + toString(this.warnings_1) + ')';
  };
  protoOf(Ready).hashCode = function () {
    var result = hashCode(this.changes_1);
    result = imul(result, 31) + hashCode(this.warnings_1) | 0;
    return result;
  };
  protoOf(Ready).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof Ready))
      return false;
    if (!equals(this.changes_1, other.changes_1))
      return false;
    if (!equals(this.warnings_1, other.warnings_1))
      return false;
    return true;
  };
  function Refused(problems) {
    this.problems_1 = problems;
  }
  protoOf(Refused).toString = function () {
    return 'Refused(problems=' + toString(this.problems_1) + ')';
  };
  protoOf(Refused).hashCode = function () {
    return hashCode(this.problems_1);
  };
  protoOf(Refused).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof Refused))
      return false;
    if (!equals(this.problems_1, other.problems_1))
      return false;
    return true;
  };
  function plan(snapshot, edit, deviceNewestWrite) {
    deviceNewestWrite = deviceNewestWrite === VOID ? null : deviceNewestWrite;
    var tmp0_elvis_lhs = edit.edit_1;
    var tmp;
    if (tmp0_elvis_lhs == null) {
      return new Refused(listOf(new ProgramProblem(ProblemCode_NO_EDIT_MARKER_getInstance(), 'edit')));
    } else {
      tmp = tmp0_elvis_lhs;
    }
    var marker = tmp;
    if (!(edit.program_1.id_1 === snapshot.program_1.id_1)) {
      return new Refused(listOf(new ProgramProblem(ProblemCode_PROGRAM_MISMATCH_getInstance(), 'program.id', edit.program_1.id_1)));
    }
    if (!(snapshot.program_1.deletedAt_1 == null)) {
      return new Refused(listOf(new ProgramProblem(ProblemCode_PROGRAM_MISSING_getInstance(), 'program.id', edit.program_1.id_1)));
    }
    if (!(edit.program_1.deletedAt_1 == null)) {
      return new Refused(listOf(new ProgramProblem(ProblemCode_MALFORMED_getInstance(), 'program.deletedAt')));
    }
    var invalid = validate(edit);
    // Inline function 'kotlin.collections.isNotEmpty' call
    if (!invalid.isEmpty_y1axqb_k$())
      return new Refused(invalid);
    var fromDocument = latestWrite(snapshot);
    var tmp_0;
    if (deviceNewestWrite == null) {
      tmp_0 = fromDocument;
    } else {
      // Inline function 'kotlin.comparisons.maxOf' call
      tmp_0 = compare(fromDocument, deviceNewestWrite) >= 0 ? fromDocument : deviceNewestWrite;
    }
    var newestWrite = tmp_0;
    if (compare(newestWrite, marker.baseExportedAt_1) > 0) {
      return new Refused(listOf(new ProgramProblem(ProblemCode_STALE_BASE_getInstance(), 'edit.baseExportedAt', newestWrite.toString())));
    }
    // Inline function 'kotlin.collections.mutableListOf' call
    var changes = ArrayList_init_$Create$_0();
    // Inline function 'kotlin.collections.mutableListOf' call
    var warnings = ArrayList_init_$Create$_0();
    if (!(snapshot.program_1.name_1 === edit.program_1.name_1)) {
      // Inline function 'kotlin.collections.plusAssign' call
      var element = new Change(ChangeKind_PROGRAM_RENAMED_getInstance(), edit.program_1.id_1, edit.program_1.name_1);
      changes.add_utx5q5_k$(element);
    }
    // Inline function 'kotlin.collections.associateBy' call
    var this_0 = snapshot.slots_1;
    var capacity = coerceAtLeast(mapCapacity(collectionSizeOrDefault(this_0, 10)), 16);
    // Inline function 'kotlin.collections.associateByTo' call
    var destination = LinkedHashMap_init_$Create$(capacity);
    var _iterator__ex2g4s = this_0.iterator_jk1svi_k$();
    while (_iterator__ex2g4s.hasNext_bitz1p_k$()) {
      var element_0 = _iterator__ex2g4s.next_20eer_k$();
      var tmp$ret$7 = element_0.id_1;
      destination.put_4fpzoq_k$(tmp$ret$7, element_0);
    }
    var snapshotSlots = destination;
    // Inline function 'kotlin.collections.map' call
    var this_1 = edit.slots_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(this_1, 10));
    var _iterator__ex2g4s_0 = this_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_0.hasNext_bitz1p_k$()) {
      var item = _iterator__ex2g4s_0.next_20eer_k$();
      var tmp$ret$10 = item.id_1;
      destination_0.add_utx5q5_k$(tmp$ret$10);
    }
    var editSlotIds = toSet(destination_0);
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_1 = edit.slots_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_1.hasNext_bitz1p_k$()) {
      var element_1 = _iterator__ex2g4s_1.next_20eer_k$();
      var old = snapshotSlots.get_wei43m_k$(element_1.id_1);
      var tmp0 = edit.templates_1;
      var tmp$ret$13;
      $l$block: {
        // Inline function 'kotlin.collections.firstOrNull' call
        var _iterator__ex2g4s_2 = tmp0.iterator_jk1svi_k$();
        while (_iterator__ex2g4s_2.hasNext_bitz1p_k$()) {
          var element_2 = _iterator__ex2g4s_2.next_20eer_k$();
          if (element_2.id_1 === element_1.templateId_1) {
            tmp$ret$13 = element_2;
            break $l$block;
          }
        }
        tmp$ret$13 = null;
      }
      var tmp0_safe_receiver = tmp$ret$13;
      var name = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.name_1;
      if (old == null) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element_3 = new Change(ChangeKind_SLOT_ADDED_getInstance(), element_1.id_1, name);
        changes.add_utx5q5_k$(element_3);
      } else if (!sameSlot(old, element_1)) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element_4 = new Change(ChangeKind_SLOT_UPDATED_getInstance(), element_1.id_1, name);
        changes.add_utx5q5_k$(element_4);
      }
    }
    // Inline function 'kotlin.collections.filter' call
    var tmp0_0 = snapshot.slots_1;
    // Inline function 'kotlin.collections.filterTo' call
    var destination_1 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_3 = tmp0_0.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_3.hasNext_bitz1p_k$()) {
      var element_5 = _iterator__ex2g4s_3.next_20eer_k$();
      if (!editSlotIds.contains_aljjnj_k$(element_5.id_1)) {
        destination_1.add_utx5q5_k$(element_5);
      }
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_4 = destination_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_4.hasNext_bitz1p_k$()) {
      var element_6 = _iterator__ex2g4s_4.next_20eer_k$();
      var tmp0_1 = snapshot.templates_1;
      var tmp$ret$22;
      $l$block_0: {
        // Inline function 'kotlin.collections.firstOrNull' call
        var _iterator__ex2g4s_5 = tmp0_1.iterator_jk1svi_k$();
        while (_iterator__ex2g4s_5.hasNext_bitz1p_k$()) {
          var element_7 = _iterator__ex2g4s_5.next_20eer_k$();
          if (element_7.id_1 === element_6.templateId_1) {
            tmp$ret$22 = element_7;
            break $l$block_0;
          }
        }
        tmp$ret$22 = null;
      }
      var tmp0_safe_receiver_0 = tmp$ret$22;
      var name_0 = tmp0_safe_receiver_0 == null ? null : tmp0_safe_receiver_0.name_1;
      // Inline function 'kotlin.collections.plusAssign' call
      var element_8 = new Change(ChangeKind_SLOT_REMOVED_getInstance(), element_6.id_1, name_0);
      changes.add_utx5q5_k$(element_8);
    }
    // Inline function 'kotlin.collections.associateBy' call
    var this_2 = snapshot.templates_1;
    var capacity_0 = coerceAtLeast(mapCapacity(collectionSizeOrDefault(this_2, 10)), 16);
    // Inline function 'kotlin.collections.associateByTo' call
    var destination_2 = LinkedHashMap_init_$Create$(capacity_0);
    var _iterator__ex2g4s_6 = this_2.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_6.hasNext_bitz1p_k$()) {
      var element_9 = _iterator__ex2g4s_6.next_20eer_k$();
      var tmp$ret$27 = element_9.id_1;
      destination_2.put_4fpzoq_k$(tmp$ret$27, element_9);
    }
    var snapshotTemplates = destination_2;
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_7 = edit.templates_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_7.hasNext_bitz1p_k$()) {
      var element_10 = _iterator__ex2g4s_7.next_20eer_k$();
      var old_0 = snapshotTemplates.get_wei43m_k$(element_10.id_1);
      if (old_0 == null) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element_11 = new Change(ChangeKind_TEMPLATE_ADDED_getInstance(), element_10.id_1, element_10.name_1);
        changes.add_utx5q5_k$(element_11);
      } else if (!sameTemplate(old_0, element_10)) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element_12 = new Change(ChangeKind_TEMPLATE_UPDATED_getInstance(), element_10.id_1, element_10.name_1);
        changes.add_utx5q5_k$(element_12);
      }
    }
    // Inline function 'kotlin.collections.map' call
    var this_3 = edit.slots_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_3 = ArrayList_init_$Create$(collectionSizeOrDefault(this_3, 10));
    var _iterator__ex2g4s_8 = this_3.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_8.hasNext_bitz1p_k$()) {
      var item_0 = _iterator__ex2g4s_8.next_20eer_k$();
      var tmp$ret$34 = item_0.templateId_1;
      destination_3.add_utx5q5_k$(tmp$ret$34);
    }
    var editSlotTemplateIds = toSet(destination_3);
    // Inline function 'kotlin.collections.filter' call
    var tmp0_2 = snapshot.slots_1;
    // Inline function 'kotlin.collections.filterTo' call
    var destination_4 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_9 = tmp0_2.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_9.hasNext_bitz1p_k$()) {
      var element_13 = _iterator__ex2g4s_9.next_20eer_k$();
      if (!editSlotIds.contains_aljjnj_k$(element_13.id_1)) {
        destination_4.add_utx5q5_k$(element_13);
      }
    }
    // Inline function 'kotlin.collections.map' call
    // Inline function 'kotlin.collections.mapTo' call
    var destination_5 = ArrayList_init_$Create$(collectionSizeOrDefault(destination_4, 10));
    var _iterator__ex2g4s_10 = destination_4.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_10.hasNext_bitz1p_k$()) {
      var item_1 = _iterator__ex2g4s_10.next_20eer_k$();
      var tmp$ret$40 = item_1.templateId_1;
      destination_5.add_utx5q5_k$(tmp$ret$40);
    }
    // Inline function 'kotlin.collections.filter' call
    // Inline function 'kotlin.collections.filterTo' call
    var destination_6 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_11 = destination_5.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_11.hasNext_bitz1p_k$()) {
      var element_14 = _iterator__ex2g4s_11.next_20eer_k$();
      if (!editSlotTemplateIds.contains_aljjnj_k$(element_14)) {
        destination_6.add_utx5q5_k$(element_14);
      }
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_12 = distinct(destination_6).iterator_jk1svi_k$();
    while (_iterator__ex2g4s_12.hasNext_bitz1p_k$()) {
      var element_15 = _iterator__ex2g4s_12.next_20eer_k$();
      var tmp_1 = WarningCode_WORKOUT_KEPT_getInstance();
      var tmp0_3 = snapshot.templates_1;
      var tmp$ret$46;
      $l$block_1: {
        // Inline function 'kotlin.collections.firstOrNull' call
        var _iterator__ex2g4s_13 = tmp0_3.iterator_jk1svi_k$();
        while (_iterator__ex2g4s_13.hasNext_bitz1p_k$()) {
          var element_16 = _iterator__ex2g4s_13.next_20eer_k$();
          if (element_16.id_1 === element_15) {
            tmp$ret$46 = element_16;
            break $l$block_1;
          }
        }
        tmp$ret$46 = null;
      }
      var tmp0_safe_receiver_1 = tmp$ret$46;
      // Inline function 'kotlin.collections.plusAssign' call
      var element_17 = new PlanWarning(tmp_1, element_15, tmp0_safe_receiver_1 == null ? null : tmp0_safe_receiver_1.name_1);
      warnings.add_utx5q5_k$(element_17);
    }
    // Inline function 'kotlin.collections.map' call
    var this_4 = edit.templates_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_7 = ArrayList_init_$Create$(collectionSizeOrDefault(this_4, 10));
    var _iterator__ex2g4s_14 = this_4.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_14.hasNext_bitz1p_k$()) {
      var item_2 = _iterator__ex2g4s_14.next_20eer_k$();
      var tmp$ret$51 = item_2.id_1;
      destination_7.add_utx5q5_k$(tmp$ret$51);
    }
    var editTemplateIds = toSet(destination_7);
    // Inline function 'kotlin.collections.map' call
    var this_5 = edit.templateExercises_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_8 = ArrayList_init_$Create$(collectionSizeOrDefault(this_5, 10));
    var _iterator__ex2g4s_15 = this_5.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_15.hasNext_bitz1p_k$()) {
      var item_3 = _iterator__ex2g4s_15.next_20eer_k$();
      var tmp$ret$54 = item_3.id_1;
      destination_8.add_utx5q5_k$(tmp$ret$54);
    }
    var editTemplateExerciseIds = toSet(destination_8);
    // Inline function 'kotlin.collections.associateBy' call
    var this_6 = snapshot.templateExercises_1;
    var capacity_1 = coerceAtLeast(mapCapacity(collectionSizeOrDefault(this_6, 10)), 16);
    // Inline function 'kotlin.collections.associateByTo' call
    var destination_9 = LinkedHashMap_init_$Create$(capacity_1);
    var _iterator__ex2g4s_16 = this_6.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_16.hasNext_bitz1p_k$()) {
      var element_18 = _iterator__ex2g4s_16.next_20eer_k$();
      var tmp$ret$57 = element_18.id_1;
      destination_9.put_4fpzoq_k$(tmp$ret$57, element_18);
    }
    var snapshotTemplateExercises = destination_9;
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_17 = edit.templateExercises_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_17.hasNext_bitz1p_k$()) {
      var element_19 = _iterator__ex2g4s_17.next_20eer_k$();
      var old_1 = snapshotTemplateExercises.get_wei43m_k$(element_19.id_1);
      if (old_1 == null) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element_20 = new Change(ChangeKind_TEMPLATE_EXERCISE_ADDED_getInstance(), element_19.id_1, null);
        changes.add_utx5q5_k$(element_20);
      } else if (!sameTemplateExercise(old_1, element_19)) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element_21 = new Change(ChangeKind_TEMPLATE_EXERCISE_UPDATED_getInstance(), element_19.id_1, null);
        changes.add_utx5q5_k$(element_21);
      }
    }
    // Inline function 'kotlin.collections.filter' call
    var tmp0_4 = snapshot.templateExercises_1;
    // Inline function 'kotlin.collections.filterTo' call
    var destination_10 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_18 = tmp0_4.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_18.hasNext_bitz1p_k$()) {
      var element_22 = _iterator__ex2g4s_18.next_20eer_k$();
      if (!editTemplateExerciseIds.contains_aljjnj_k$(element_22.id_1) && editTemplateIds.contains_aljjnj_k$(element_22.templateId_1)) {
        destination_10.add_utx5q5_k$(element_22);
      }
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_19 = destination_10.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_19.hasNext_bitz1p_k$()) {
      var element_23 = _iterator__ex2g4s_19.next_20eer_k$();
      // Inline function 'kotlin.collections.plusAssign' call
      var element_24 = new Change(ChangeKind_TEMPLATE_EXERCISE_REMOVED_getInstance(), element_23.id_1, null);
      changes.add_utx5q5_k$(element_24);
    }
    // Inline function 'kotlin.collections.associateBy' call
    var this_7 = snapshot.templateSets_1;
    var capacity_2 = coerceAtLeast(mapCapacity(collectionSizeOrDefault(this_7, 10)), 16);
    // Inline function 'kotlin.collections.associateByTo' call
    var destination_11 = LinkedHashMap_init_$Create$(capacity_2);
    var _iterator__ex2g4s_20 = this_7.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_20.hasNext_bitz1p_k$()) {
      var element_25 = _iterator__ex2g4s_20.next_20eer_k$();
      var tmp$ret$70 = element_25.id_1;
      destination_11.put_4fpzoq_k$(tmp$ret$70, element_25);
    }
    var snapshotSets = destination_11;
    // Inline function 'kotlin.collections.map' call
    var this_8 = edit.templateSets_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_12 = ArrayList_init_$Create$(collectionSizeOrDefault(this_8, 10));
    var _iterator__ex2g4s_21 = this_8.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_21.hasNext_bitz1p_k$()) {
      var item_4 = _iterator__ex2g4s_21.next_20eer_k$();
      var tmp$ret$73 = item_4.id_1;
      destination_12.add_utx5q5_k$(tmp$ret$73);
    }
    var editSetIds = toSet(destination_12);
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_22 = edit.templateSets_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_22.hasNext_bitz1p_k$()) {
      var element_26 = _iterator__ex2g4s_22.next_20eer_k$();
      var old_2 = snapshotSets.get_wei43m_k$(element_26.id_1);
      if (old_2 == null) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element_27 = new Change(ChangeKind_SET_ADDED_getInstance(), element_26.id_1, null);
        changes.add_utx5q5_k$(element_27);
      } else if (!sameSet(old_2, element_26)) {
        // Inline function 'kotlin.collections.plusAssign' call
        var element_28 = new Change(ChangeKind_SET_UPDATED_getInstance(), element_26.id_1, null);
        changes.add_utx5q5_k$(element_28);
      }
    }
    // Inline function 'kotlin.collections.filter' call
    var tmp0_5 = snapshot.templateSets_1;
    // Inline function 'kotlin.collections.filterTo' call
    var destination_13 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_23 = tmp0_5.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_23.hasNext_bitz1p_k$()) {
      var element_29 = _iterator__ex2g4s_23.next_20eer_k$();
      if (!editSetIds.contains_aljjnj_k$(element_29.id_1) && editTemplateExerciseIds.contains_aljjnj_k$(element_29.templateExerciseId_1)) {
        destination_13.add_utx5q5_k$(element_29);
      }
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_24 = destination_13.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_24.hasNext_bitz1p_k$()) {
      var element_30 = _iterator__ex2g4s_24.next_20eer_k$();
      // Inline function 'kotlin.collections.plusAssign' call
      var element_31 = new Change(ChangeKind_SET_REMOVED_getInstance(), element_30.id_1, null);
      changes.add_utx5q5_k$(element_31);
    }
    // Inline function 'kotlin.collections.map' call
    var this_9 = snapshot.exercises_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_14 = ArrayList_init_$Create$(collectionSizeOrDefault(this_9, 10));
    var _iterator__ex2g4s_25 = this_9.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_25.hasNext_bitz1p_k$()) {
      var item_5 = _iterator__ex2g4s_25.next_20eer_k$();
      var tmp$ret$86 = item_5.id_1;
      destination_14.add_utx5q5_k$(tmp$ret$86);
    }
    var snapshotExerciseIds = toSet(destination_14);
    // Inline function 'kotlin.collections.filter' call
    var tmp0_6 = edit.exercises_1;
    // Inline function 'kotlin.collections.filterTo' call
    var destination_15 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_26 = tmp0_6.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_26.hasNext_bitz1p_k$()) {
      var element_32 = _iterator__ex2g4s_26.next_20eer_k$();
      if (!snapshotExerciseIds.contains_aljjnj_k$(element_32.id_1)) {
        destination_15.add_utx5q5_k$(element_32);
      }
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_27 = destination_15.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_27.hasNext_bitz1p_k$()) {
      var element_33 = _iterator__ex2g4s_27.next_20eer_k$();
      // Inline function 'kotlin.collections.plusAssign' call
      var element_34 = new Change(ChangeKind_EXERCISE_ADDED_getInstance(), element_33.id_1, element_33.name_1);
      changes.add_utx5q5_k$(element_34);
      // Inline function 'kotlin.collections.plusAssign' call
      var element_35 = new PlanWarning(WarningCode_NEW_MOVEMENT_getInstance(), element_33.id_1, element_33.name_1);
      warnings.add_utx5q5_k$(element_35);
    }
    return new Ready(changes, warnings);
  }
  function latestWrite(document) {
    var latest = {_v: document.program_1.updatedAt_1};
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s = document.slots_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s.hasNext_bitz1p_k$()) {
      var element = _iterator__ex2g4s.next_20eer_k$();
      latestWrite$consider(latest, element.updatedAt_1);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_0 = document.templates_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_0.hasNext_bitz1p_k$()) {
      var element_0 = _iterator__ex2g4s_0.next_20eer_k$();
      latestWrite$consider(latest, element_0.updatedAt_1);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_1 = document.templateExercises_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_1.hasNext_bitz1p_k$()) {
      var element_1 = _iterator__ex2g4s_1.next_20eer_k$();
      latestWrite$consider(latest, element_1.updatedAt_1);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_2 = document.templateSets_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_2.hasNext_bitz1p_k$()) {
      var element_2 = _iterator__ex2g4s_2.next_20eer_k$();
      latestWrite$consider(latest, element_2.updatedAt_1);
    }
    return latest._v;
  }
  function sameSlot(a, b) {
    return a.programId_1 === b.programId_1 && a.templateId_1 === b.templateId_1 && a.position_1 === b.position_1 && equals(a.weekday_1, b.weekday_1);
  }
  function sameTemplate(a, b) {
    return a.name_1 === b.name_1 && a.versionOf_1 == b.versionOf_1;
  }
  function sameTemplateExercise(a, b) {
    return a.templateId_1 === b.templateId_1 && a.exerciseId_1 === b.exerciseId_1 && a.position_1 === b.position_1 && a.restSeconds_1 == b.restSeconds_1 && a.techniqueNote_1 == b.techniqueNote_1 && a.targetRpeHalves_1 == b.targetRpeHalves_1 && a.supersetGroup_1 == b.supersetGroup_1;
  }
  function sameSet(a, b) {
    return a.templateExerciseId_1 === b.templateExerciseId_1 && a.setIndex_1 === b.setIndex_1 && a.role_1.equals(b.role_1) && equals(a.targetWeightGrams_1, b.targetWeightGrams_1) && equals(a.targetAssistanceGrams_1, b.targetAssistanceGrams_1) && a.targetRepsMin_1 == b.targetRepsMin_1 && a.targetRepsMax_1 == b.targetRepsMax_1 && a.targetRepsCurrent_1 == b.targetRepsCurrent_1 && equals(a.dropValueGrams_1, b.dropValueGrams_1) && a.note_1 == b.note_1;
  }
  function latestWrite$consider(latest, time) {
    if (compare(time, latest._v) > 0) {
      latest._v = time;
    }
  }
  function ChangeKind_PROGRAM_RENAMED_getInstance() {
    static_init_2();
    return ChangeKind_PROGRAM_RENAMED_instance;
  }
  function ChangeKind_SLOT_ADDED_getInstance() {
    static_init_2();
    return ChangeKind_SLOT_ADDED_instance;
  }
  function ChangeKind_SLOT_UPDATED_getInstance() {
    static_init_2();
    return ChangeKind_SLOT_UPDATED_instance;
  }
  function ChangeKind_SLOT_REMOVED_getInstance() {
    static_init_2();
    return ChangeKind_SLOT_REMOVED_instance;
  }
  function ChangeKind_TEMPLATE_ADDED_getInstance() {
    static_init_2();
    return ChangeKind_TEMPLATE_ADDED_instance;
  }
  function ChangeKind_TEMPLATE_UPDATED_getInstance() {
    static_init_2();
    return ChangeKind_TEMPLATE_UPDATED_instance;
  }
  function ChangeKind_TEMPLATE_EXERCISE_ADDED_getInstance() {
    static_init_2();
    return ChangeKind_TEMPLATE_EXERCISE_ADDED_instance;
  }
  function ChangeKind_TEMPLATE_EXERCISE_UPDATED_getInstance() {
    static_init_2();
    return ChangeKind_TEMPLATE_EXERCISE_UPDATED_instance;
  }
  function ChangeKind_TEMPLATE_EXERCISE_REMOVED_getInstance() {
    static_init_2();
    return ChangeKind_TEMPLATE_EXERCISE_REMOVED_instance;
  }
  function ChangeKind_SET_ADDED_getInstance() {
    static_init_2();
    return ChangeKind_SET_ADDED_instance;
  }
  function ChangeKind_SET_UPDATED_getInstance() {
    static_init_2();
    return ChangeKind_SET_UPDATED_instance;
  }
  function ChangeKind_SET_REMOVED_getInstance() {
    static_init_2();
    return ChangeKind_SET_REMOVED_instance;
  }
  function ChangeKind_EXERCISE_ADDED_getInstance() {
    static_init_2();
    return ChangeKind_EXERCISE_ADDED_instance;
  }
  function WarningCode_NEW_MOVEMENT_getInstance() {
    static_init_3();
    return WarningCode_NEW_MOVEMENT_instance;
  }
  function WarningCode_WORKOUT_KEPT_getInstance() {
    static_init_3();
    return WarningCode_WORKOUT_KEPT_instance;
  }
  function validate(document) {
    // Inline function 'kotlin.collections.mutableListOf' call
    var problems = ArrayList_init_$Create$_0();
    if (isBlank(document.program_1.id_1)) {
      validate$refuse(problems, ProblemCode_MALFORMED_getInstance(), 'program.id');
    }
    if (isBlank(document.program_1.name_1)) {
      validate$refuse(problems, ProblemCode_EMPTY_NAME_getInstance(), 'program.name');
    }
    // Inline function 'kotlin.collections.map' call
    var this_0 = document.templates_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s = this_0.iterator_jk1svi_k$();
    while (_iterator__ex2g4s.hasNext_bitz1p_k$()) {
      var item = _iterator__ex2g4s.next_20eer_k$();
      var tmp$ret$3 = item.id_1;
      destination.add_utx5q5_k$(tmp$ret$3);
    }
    var templateIds = toSet(destination);
    // Inline function 'kotlin.collections.map' call
    var this_1 = document.exercises_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(this_1, 10));
    var _iterator__ex2g4s_0 = this_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_0.hasNext_bitz1p_k$()) {
      var item_0 = _iterator__ex2g4s_0.next_20eer_k$();
      var tmp$ret$6 = item_0.id_1;
      destination_0.add_utx5q5_k$(tmp$ret$6);
    }
    var exerciseIds = toSet(destination_0);
    // Inline function 'kotlin.collections.associateBy' call
    var this_2 = document.exercises_1;
    var capacity = coerceAtLeast(mapCapacity(collectionSizeOrDefault(this_2, 10)), 16);
    // Inline function 'kotlin.collections.associateByTo' call
    var destination_1 = LinkedHashMap_init_$Create$(capacity);
    var _iterator__ex2g4s_1 = this_2.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_1.hasNext_bitz1p_k$()) {
      var element = _iterator__ex2g4s_1.next_20eer_k$();
      var tmp$ret$9 = element.id_1;
      destination_1.put_4fpzoq_k$(tmp$ret$9, element);
    }
    var exerciseById = destination_1;
    // Inline function 'kotlin.collections.map' call
    var this_3 = document.templateExercises_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_2 = ArrayList_init_$Create$(collectionSizeOrDefault(this_3, 10));
    var _iterator__ex2g4s_2 = this_3.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_2.hasNext_bitz1p_k$()) {
      var item_1 = _iterator__ex2g4s_2.next_20eer_k$();
      var tmp$ret$12 = item_1.id_1;
      destination_2.add_utx5q5_k$(tmp$ret$12);
    }
    var templateExerciseIds = toSet(destination_2);
    // Inline function 'kotlin.collections.map' call
    var this_4 = document.templates_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_3 = ArrayList_init_$Create$(collectionSizeOrDefault(this_4, 10));
    var _iterator__ex2g4s_3 = this_4.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_3.hasNext_bitz1p_k$()) {
      var item_2 = _iterator__ex2g4s_3.next_20eer_k$();
      var tmp$ret$15 = item_2.id_1;
      destination_3.add_utx5q5_k$(tmp$ret$15);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_4 = duplicates(destination_3).iterator_jk1svi_k$();
    while (_iterator__ex2g4s_4.hasNext_bitz1p_k$()) {
      var element_0 = _iterator__ex2g4s_4.next_20eer_k$();
      validate$refuse(problems, ProblemCode_DUPLICATE_ID_getInstance(), 'templates', element_0);
    }
    // Inline function 'kotlin.collections.map' call
    var this_5 = document.slots_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_4 = ArrayList_init_$Create$(collectionSizeOrDefault(this_5, 10));
    var _iterator__ex2g4s_5 = this_5.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_5.hasNext_bitz1p_k$()) {
      var item_3 = _iterator__ex2g4s_5.next_20eer_k$();
      var tmp$ret$20 = item_3.id_1;
      destination_4.add_utx5q5_k$(tmp$ret$20);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_6 = duplicates(destination_4).iterator_jk1svi_k$();
    while (_iterator__ex2g4s_6.hasNext_bitz1p_k$()) {
      var element_1 = _iterator__ex2g4s_6.next_20eer_k$();
      validate$refuse(problems, ProblemCode_DUPLICATE_ID_getInstance(), 'slots', element_1);
    }
    // Inline function 'kotlin.collections.map' call
    var this_6 = document.templateExercises_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_5 = ArrayList_init_$Create$(collectionSizeOrDefault(this_6, 10));
    var _iterator__ex2g4s_7 = this_6.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_7.hasNext_bitz1p_k$()) {
      var item_4 = _iterator__ex2g4s_7.next_20eer_k$();
      var tmp$ret$25 = item_4.id_1;
      destination_5.add_utx5q5_k$(tmp$ret$25);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_8 = duplicates(destination_5).iterator_jk1svi_k$();
    while (_iterator__ex2g4s_8.hasNext_bitz1p_k$()) {
      var element_2 = _iterator__ex2g4s_8.next_20eer_k$();
      validate$refuse(problems, ProblemCode_DUPLICATE_ID_getInstance(), 'templateExercises', element_2);
    }
    // Inline function 'kotlin.collections.map' call
    var this_7 = document.templateSets_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_6 = ArrayList_init_$Create$(collectionSizeOrDefault(this_7, 10));
    var _iterator__ex2g4s_9 = this_7.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_9.hasNext_bitz1p_k$()) {
      var item_5 = _iterator__ex2g4s_9.next_20eer_k$();
      var tmp$ret$30 = item_5.id_1;
      destination_6.add_utx5q5_k$(tmp$ret$30);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_10 = duplicates(destination_6).iterator_jk1svi_k$();
    while (_iterator__ex2g4s_10.hasNext_bitz1p_k$()) {
      var element_3 = _iterator__ex2g4s_10.next_20eer_k$();
      validate$refuse(problems, ProblemCode_DUPLICATE_ID_getInstance(), 'templateSets', element_3);
    }
    // Inline function 'kotlin.collections.map' call
    var this_8 = document.exercises_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_7 = ArrayList_init_$Create$(collectionSizeOrDefault(this_8, 10));
    var _iterator__ex2g4s_11 = this_8.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_11.hasNext_bitz1p_k$()) {
      var item_6 = _iterator__ex2g4s_11.next_20eer_k$();
      var tmp$ret$35 = item_6.id_1;
      destination_7.add_utx5q5_k$(tmp$ret$35);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_12 = duplicates(destination_7).iterator_jk1svi_k$();
    while (_iterator__ex2g4s_12.hasNext_bitz1p_k$()) {
      var element_4 = _iterator__ex2g4s_12.next_20eer_k$();
      validate$refuse(problems, ProblemCode_DUPLICATE_ID_getInstance(), 'exercises', element_4);
    }
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index = 0;
    var _iterator__ex2g4s_13 = document.templates_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_13.hasNext_bitz1p_k$()) {
      var item_7 = _iterator__ex2g4s_13.next_20eer_k$();
      var _unary__edvuaz = index;
      index = _unary__edvuaz + 1 | 0;
      var index_0 = checkIndexOverflow(_unary__edvuaz);
      if (isBlank(item_7.name_1)) {
        validate$refuse(problems, ProblemCode_EMPTY_NAME_getInstance(), 'templates[' + index_0 + '].name');
      }
    }
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index_1 = 0;
    var _iterator__ex2g4s_14 = document.exercises_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_14.hasNext_bitz1p_k$()) {
      var item_8 = _iterator__ex2g4s_14.next_20eer_k$();
      var _unary__edvuaz_0 = index_1;
      index_1 = _unary__edvuaz_0 + 1 | 0;
      var index_2 = checkIndexOverflow(_unary__edvuaz_0);
      if (isBlank(item_8.name_1)) {
        validate$refuse(problems, ProblemCode_EMPTY_NAME_getInstance(), 'exercises[' + index_2 + '].name');
      }
      if (!(item_8.rowKind_1 === 'MOVEMENT') && !(item_8.rowKind_1 === 'CATEGORY')) {
        validate$refuse(problems, ProblemCode_MALFORMED_getInstance(), 'exercises[' + index_2 + '].rowKind', item_8.rowKind_1);
      }
    }
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index_3 = 0;
    var _iterator__ex2g4s_15 = document.slots_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_15.hasNext_bitz1p_k$()) {
      var item_9 = _iterator__ex2g4s_15.next_20eer_k$();
      var _unary__edvuaz_1 = index_3;
      index_3 = _unary__edvuaz_1 + 1 | 0;
      var index_4 = checkIndexOverflow(_unary__edvuaz_1);
      if (!(item_9.programId_1 === document.program_1.id_1)) {
        validate$refuse(problems, ProblemCode_MALFORMED_getInstance(), 'slots[' + index_4 + '].programId', item_9.programId_1);
      }
      if (!templateIds.contains_aljjnj_k$(item_9.templateId_1)) {
        validate$refuse(problems, ProblemCode_DANGLING_SLOT_TEMPLATE_getInstance(), 'slots[' + index_4 + '].templateId', item_9.templateId_1);
      }
      if (item_9.position_1 < 0) {
        validate$refuse(problems, ProblemCode_INVALID_POSITION_getInstance(), 'slots[' + index_4 + '].position');
      }
    }
    // Inline function 'kotlin.collections.map' call
    var this_9 = document.slots_1;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_8 = ArrayList_init_$Create$(collectionSizeOrDefault(this_9, 10));
    var _iterator__ex2g4s_16 = this_9.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_16.hasNext_bitz1p_k$()) {
      var item_10 = _iterator__ex2g4s_16.next_20eer_k$();
      var tmp$ret$46 = item_10.position_1;
      destination_8.add_utx5q5_k$(tmp$ret$46);
    }
    // Inline function 'kotlin.collections.forEach' call
    var _iterator__ex2g4s_17 = duplicates(destination_8).iterator_jk1svi_k$();
    while (_iterator__ex2g4s_17.hasNext_bitz1p_k$()) {
      var element_5 = _iterator__ex2g4s_17.next_20eer_k$();
      validate$refuse(problems, ProblemCode_INVALID_POSITION_getInstance(), 'slots', element_5);
    }
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index_5 = 0;
    var _iterator__ex2g4s_18 = document.templateExercises_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_18.hasNext_bitz1p_k$()) {
      var item_11 = _iterator__ex2g4s_18.next_20eer_k$();
      var _unary__edvuaz_2 = index_5;
      index_5 = _unary__edvuaz_2 + 1 | 0;
      var path = 'templateExercises[' + checkIndexOverflow(_unary__edvuaz_2) + ']';
      if (!templateIds.contains_aljjnj_k$(item_11.templateId_1)) {
        validate$refuse(problems, ProblemCode_DANGLING_TEMPLATE_EXERCISE_getInstance(), path + '.templateId', item_11.templateId_1);
      }
      if (!exerciseIds.contains_aljjnj_k$(item_11.exerciseId_1)) {
        validate$refuse(problems, ProblemCode_DANGLING_EXERCISE_getInstance(), path + '.exerciseId', item_11.exerciseId_1);
      } else {
        var tmp0_safe_receiver = exerciseById.get_wei43m_k$(item_11.exerciseId_1);
        if (!((tmp0_safe_receiver == null ? null : tmp0_safe_receiver.rowKind_1) === 'MOVEMENT')) {
          validate$refuse(problems, ProblemCode_DANGLING_EXERCISE_getInstance(), path + '.exerciseId', item_11.exerciseId_1);
        }
      }
      if (item_11.position_1 < 0) {
        validate$refuse(problems, ProblemCode_INVALID_POSITION_getInstance(), path + '.position');
      }
      var tmp1_safe_receiver = item_11.restSeconds_1;
      if (tmp1_safe_receiver == null)
        null;
      else {
        // Inline function 'kotlin.let' call
        if (tmp1_safe_receiver < 0 || tmp1_safe_receiver > 3600) {
          validate$refuse(problems, ProblemCode_INVALID_REST_getInstance(), path + '.restSeconds');
        }
      }
      var tmp2_safe_receiver = item_11.targetRpeHalves_1;
      if (tmp2_safe_receiver == null)
        null;
      else {
        // Inline function 'kotlin.let' call
        if (!(1 <= tmp2_safe_receiver ? tmp2_safe_receiver <= 20 : false)) {
          validate$refuse(problems, ProblemCode_INVALID_RPE_getInstance(), path + '.targetRpeHalves');
        }
      }
    }
    // Inline function 'kotlin.collections.groupBy' call
    var tmp0 = document.templateExercises_1;
    // Inline function 'kotlin.collections.groupByTo' call
    var destination_9 = LinkedHashMap_init_$Create$_0();
    var _iterator__ex2g4s_19 = tmp0.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_19.hasNext_bitz1p_k$()) {
      var element_6 = _iterator__ex2g4s_19.next_20eer_k$();
      var key = element_6.templateId_1;
      // Inline function 'kotlin.collections.getOrPut' call
      var value = destination_9.get_wei43m_k$(key);
      var tmp;
      if (value == null) {
        var answer = ArrayList_init_$Create$_0();
        destination_9.put_4fpzoq_k$(key, answer);
        tmp = answer;
      } else {
        tmp = value;
      }
      var list = tmp;
      list.add_utx5q5_k$(element_6);
    }
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s_20 = destination_9.get_entries_p20ztl_k$().iterator_jk1svi_k$();
    while (_iterator__ex2g4s_20.hasNext_bitz1p_k$()) {
      var element_7 = _iterator__ex2g4s_20.next_20eer_k$();
      // Inline function 'kotlin.collections.component1' call
      var templateId = element_7.get_key_18j28a_k$();
      // Inline function 'kotlin.collections.component2' call
      var rows = element_7.get_value_j01efc_k$();
      // Inline function 'kotlin.collections.map' call
      // Inline function 'kotlin.collections.mapTo' call
      var destination_10 = ArrayList_init_$Create$(collectionSizeOrDefault(rows, 10));
      var _iterator__ex2g4s_21 = rows.iterator_jk1svi_k$();
      while (_iterator__ex2g4s_21.hasNext_bitz1p_k$()) {
        var item_12 = _iterator__ex2g4s_21.next_20eer_k$();
        var tmp$ret$67 = item_12.position_1;
        destination_10.add_utx5q5_k$(tmp$ret$67);
      }
      // Inline function 'kotlin.collections.forEach' call
      var _iterator__ex2g4s_22 = duplicates(destination_10).iterator_jk1svi_k$();
      while (_iterator__ex2g4s_22.hasNext_bitz1p_k$()) {
        var element_8 = _iterator__ex2g4s_22.next_20eer_k$();
        validate$refuse(problems, ProblemCode_INVALID_POSITION_getInstance(), 'templateExercises', templateId + '/' + element_8);
      }
    }
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index_6 = 0;
    var _iterator__ex2g4s_23 = document.templateSets_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_23.hasNext_bitz1p_k$()) {
      var item_13 = _iterator__ex2g4s_23.next_20eer_k$();
      var _unary__edvuaz_3 = index_6;
      index_6 = _unary__edvuaz_3 + 1 | 0;
      var path_0 = 'templateSets[' + checkIndexOverflow(_unary__edvuaz_3) + ']';
      if (!templateExerciseIds.contains_aljjnj_k$(item_13.templateExerciseId_1)) {
        validate$refuse(problems, ProblemCode_DANGLING_SET_getInstance(), path_0 + '.templateExerciseId', item_13.templateExerciseId_1);
      }
      if (item_13.setIndex_1 < 0) {
        validate$refuse(problems, ProblemCode_INVALID_SET_INDEX_getInstance(), path_0 + '.setIndex');
      }
      var tmp0_elvis_lhs = item_13.targetWeightGrams_1;
      if (compare(tmp0_elvis_lhs == null ? new Long(0, 0) : tmp0_elvis_lhs, new Long(0, 0)) < 0) {
        validate$refuse(problems, ProblemCode_INVALID_LOAD_getInstance(), path_0 + '.targetWeightGrams');
      }
      var tmp1_elvis_lhs = item_13.targetAssistanceGrams_1;
      if (compare(tmp1_elvis_lhs == null ? new Long(0, 0) : tmp1_elvis_lhs, new Long(0, 0)) < 0) {
        validate$refuse(problems, ProblemCode_INVALID_LOAD_getInstance(), path_0 + '.targetAssistanceGrams');
      }
      var tmp2_elvis_lhs = item_13.dropValueGrams_1;
      if (compare(tmp2_elvis_lhs == null ? new Long(0, 0) : tmp2_elvis_lhs, new Long(0, 0)) < 0) {
        validate$refuse(problems, ProblemCode_INVALID_LOAD_getInstance(), path_0 + '.dropValueGrams');
      }
      var tmp3_safe_receiver = item_13.targetRepsMin_1;
      if (tmp3_safe_receiver == null)
        null;
      else {
        // Inline function 'kotlin.let' call
        if (tmp3_safe_receiver < 1) {
          validate$refuse(problems, ProblemCode_INVALID_REPS_getInstance(), path_0 + '.targetRepsMin');
        }
      }
      var tmp4_safe_receiver = item_13.targetRepsMax_1;
      if (tmp4_safe_receiver == null)
        null;
      else {
        // Inline function 'kotlin.let' call
        if (tmp4_safe_receiver < 1) {
          validate$refuse(problems, ProblemCode_INVALID_REPS_getInstance(), path_0 + '.targetRepsMax');
        }
      }
      var min = item_13.targetRepsMin_1;
      var max = item_13.targetRepsMax_1;
      if (!(min == null) && !(max == null) && min > max) {
        validate$refuse(problems, ProblemCode_INVALID_REPS_getInstance(), path_0);
      }
      var tmp5_safe_receiver = item_13.targetRepsCurrent_1;
      if (tmp5_safe_receiver == null)
        null;
      else {
        // Inline function 'kotlin.let' call
        if (tmp5_safe_receiver < 1) {
          validate$refuse(problems, ProblemCode_INVALID_REPS_getInstance(), path_0 + '.targetRepsCurrent');
        }
        if (!(max == null) && tmp5_safe_receiver > max) {
          validate$refuse(problems, ProblemCode_INVALID_REPS_getInstance(), path_0 + '.targetRepsCurrent');
        }
        if (!(min == null) && tmp5_safe_receiver < min) {
          validate$refuse(problems, ProblemCode_INVALID_REPS_getInstance(), path_0 + '.targetRepsCurrent');
        }
      }
    }
    // Inline function 'kotlin.collections.groupBy' call
    var tmp0_0 = document.templateSets_1;
    // Inline function 'kotlin.collections.groupByTo' call
    var destination_11 = LinkedHashMap_init_$Create$_0();
    var _iterator__ex2g4s_24 = tmp0_0.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_24.hasNext_bitz1p_k$()) {
      var element_9 = _iterator__ex2g4s_24.next_20eer_k$();
      var key_0 = element_9.templateExerciseId_1;
      // Inline function 'kotlin.collections.getOrPut' call
      var value_0 = destination_11.get_wei43m_k$(key_0);
      var tmp_0;
      if (value_0 == null) {
        var answer_0 = ArrayList_init_$Create$_0();
        destination_11.put_4fpzoq_k$(key_0, answer_0);
        tmp_0 = answer_0;
      } else {
        tmp_0 = value_0;
      }
      var list_0 = tmp_0;
      list_0.add_utx5q5_k$(element_9);
    }
    // Inline function 'kotlin.collections.forEach' call
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s_25 = destination_11.get_entries_p20ztl_k$().iterator_jk1svi_k$();
    while (_iterator__ex2g4s_25.hasNext_bitz1p_k$()) {
      var element_10 = _iterator__ex2g4s_25.next_20eer_k$();
      // Inline function 'kotlin.collections.component1' call
      var plannedId = element_10.get_key_18j28a_k$();
      // Inline function 'kotlin.collections.component2' call
      var rows_0 = element_10.get_value_j01efc_k$();
      // Inline function 'kotlin.collections.map' call
      // Inline function 'kotlin.collections.mapTo' call
      var destination_12 = ArrayList_init_$Create$(collectionSizeOrDefault(rows_0, 10));
      var _iterator__ex2g4s_26 = rows_0.iterator_jk1svi_k$();
      while (_iterator__ex2g4s_26.hasNext_bitz1p_k$()) {
        var item_14 = _iterator__ex2g4s_26.next_20eer_k$();
        var tmp$ret$90 = item_14.setIndex_1;
        destination_12.add_utx5q5_k$(tmp$ret$90);
      }
      // Inline function 'kotlin.collections.forEach' call
      var _iterator__ex2g4s_27 = duplicates(destination_12).iterator_jk1svi_k$();
      while (_iterator__ex2g4s_27.hasNext_bitz1p_k$()) {
        var element_11 = _iterator__ex2g4s_27.next_20eer_k$();
        validate$refuse(problems, ProblemCode_INVALID_SET_INDEX_getInstance(), 'templateSets', plannedId + '/' + element_11);
      }
      // Inline function 'kotlin.collections.sortedBy' call
      // Inline function 'kotlin.comparisons.compareBy' call
      var tmp_1 = validate$lambda;
      var tmp$ret$94 = new sam$kotlin_Comparator$0(tmp_1);
      var tmp_2 = sortedWith(rows_0, tmp$ret$94);
      validateRuns(tmp_2, plannedId, validate$refuse$ref(problems));
    }
    return problems;
  }
  function validateRuns(ordered, plannedId, refuse) {
    // Inline function 'kotlin.collections.forEachIndexed' call
    var index = 0;
    var _iterator__ex2g4s = ordered.iterator_jk1svi_k$();
    while (_iterator__ex2g4s.hasNext_bitz1p_k$()) {
      var item = _iterator__ex2g4s.next_20eer_k$();
      var _unary__edvuaz = index;
      index = _unary__edvuaz + 1 | 0;
      var tmp0 = checkIndexOverflow(_unary__edvuaz);
      $l$block: {
        if (!item.role_1.get_isRung_ew5llb_k$()) {
          break $l$block;
        }
        var path = 'templateSets(' + plannedId + ')[' + tmp0 + ']';
        if (!(item.targetWeightGrams_1 == null) || !(item.targetAssistanceGrams_1 == null) || !(item.targetRepsMin_1 == null) || !(item.targetRepsMax_1 == null)) {
          refuse(ProblemCode_RUNG_CARRIES_TARGETS_getInstance(), path, null);
        }
        var above = getOrNull(ordered, tmp0 - 1 | 0);
        if (above == null)
          refuse(ProblemCode_RUNG_WITHOUT_ANCHOR_getInstance(), path, null);
        else if (above.role_1.equals(ProgramSetRole_WARMUP_getInstance())) {
          refuse(ProblemCode_RUNG_UNDER_WARMUP_getInstance(), path, null);
        } else if (!above.role_1.get_isRung_ew5llb_k$()) {
          var tmp;
          if (item.role_1.equals(ProgramSetRole_DROP_getInstance())) {
            var tmp0_elvis_lhs = item.dropValueGrams_1;
            tmp = compare(tmp0_elvis_lhs == null ? new Long(0, 0) : tmp0_elvis_lhs, new Long(0, 0)) <= 0;
          } else {
            tmp = false;
          }
          if (tmp) {
            refuse(ProblemCode_RUNG_WITHOUT_VALUE_getInstance(), path, null);
          }
        } else if (!above.role_1.equals(item.role_1)) {
          refuse(ProblemCode_RUN_MIXES_KINDS_getInstance(), path, null);
        }
      }
    }
  }
  function duplicates(ids) {
    // Inline function 'kotlin.collections.groupingBy' call
    var tmp$ret$0 = new duplicates$$inlined$groupingBy$1(ids);
    // Inline function 'kotlin.collections.filterValues' call
    var this_0 = eachCount(tmp$ret$0);
    var result = LinkedHashMap_init_$Create$_0();
    // Inline function 'kotlin.collections.iterator' call
    var _iterator__ex2g4s = this_0.get_entries_p20ztl_k$().iterator_jk1svi_k$();
    while (_iterator__ex2g4s.hasNext_bitz1p_k$()) {
      var entry = _iterator__ex2g4s.next_20eer_k$();
      if (entry.get_value_j01efc_k$() > 1) {
        result.put_4fpzoq_k$(entry.get_key_18j28a_k$(), entry.get_value_j01efc_k$());
      }
    }
    // Inline function 'kotlin.collections.map' call
    var this_1 = result.get_keys_wop4xp_k$();
    // Inline function 'kotlin.collections.mapTo' call
    var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_1, 10));
    var _iterator__ex2g4s_0 = this_1.iterator_jk1svi_k$();
    while (_iterator__ex2g4s_0.hasNext_bitz1p_k$()) {
      var item = _iterator__ex2g4s_0.next_20eer_k$();
      var tmp$ret$6 = toString(item);
      destination.add_utx5q5_k$(tmp$ret$6);
    }
    return destination;
  }
  function sam$kotlin_Comparator$0(function_0) {
    this.function_1 = function_0;
  }
  protoOf(sam$kotlin_Comparator$0).compare_bczr_k$ = function (a, b) {
    return this.function_1(a, b);
  };
  protoOf(sam$kotlin_Comparator$0).compare = function (a, b) {
    return this.compare_bczr_k$(a, b);
  };
  protoOf(sam$kotlin_Comparator$0).getFunctionDelegate_jtodtf_k$ = function () {
    return this.function_1;
  };
  protoOf(sam$kotlin_Comparator$0).equals = function (other) {
    var tmp;
    if (!(other == null) ? isInterface(other, Comparator) : false) {
      var tmp_0;
      if (!(other == null) ? isInterface(other, FunctionAdapter) : false) {
        tmp_0 = equals(this.getFunctionDelegate_jtodtf_k$(), other.getFunctionDelegate_jtodtf_k$());
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
    return hashCode(this.getFunctionDelegate_jtodtf_k$());
  };
  function validate$refuse(problems, code, path, detail) {
    path = path === VOID ? '' : path;
    detail = detail === VOID ? null : detail;
    // Inline function 'kotlin.collections.plusAssign' call
    var element = new ProgramProblem(code, path, detail);
    problems.add_utx5q5_k$(element);
  }
  function validate$lambda(a, b) {
    // Inline function 'kotlin.comparisons.compareValuesBy' call
    var tmp = a.setIndex_1;
    var tmp$ret$2 = b.setIndex_1;
    return compareValues(tmp, tmp$ret$2);
  }
  function validate$refuse$ref($problems) {
    return constructCallableReference(function (p0, p1, p2) {
      validate$refuse($problems, p0, p1, p2);
      return Unit_instance;
    }, 3, 0, 10, 'refuse', [$problems]);
  }
  function duplicates$$inlined$groupingBy$1($this) {
    this.$this_1 = $this;
  }
  protoOf(duplicates$$inlined$groupingBy$1).sourceIterator_2zqxcn_k$ = function () {
    return this.$this_1.iterator_jk1svi_k$();
  };
  protoOf(duplicates$$inlined$groupingBy$1).keyOf_qdhuon_k$ = function (element) {
    return element;
  };
  protoOf(duplicates$$inlined$groupingBy$1).keyOf_d1fr09_k$ = function (element) {
    return this.keyOf_qdhuon_k$(element);
  };
  //region block: post-declaration
  protoOf($serializer).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_0).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_1).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_2).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_3).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_4).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_5).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_6).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_7).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_8).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_9).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_10).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
  protoOf($serializer_11).typeParametersSerializers_fr94fx_k$ = typeParametersSerializers;
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
