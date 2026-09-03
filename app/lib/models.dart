class AuthResult {
  final String token;
  final int userId;
  final String phone;
  final String name;

  AuthResult({required this.token, required this.userId, required this.phone, required this.name});

  factory AuthResult.fromJson(Map<String, dynamic> json) => AuthResult(
        token: json['token'] as String,
        userId: (json['userId'] as num).toInt(),
        phone: json['phone'] as String? ?? '',
        name: json['name'] as String? ?? '',
      );
}

class LovedOne {
  final int id;
  final int familyId;
  final String name;
  final String? birthDate;
  final String? deathDate;
  final String? birthPlace;
  final String? bio;
  final bool isDeceased;
  final bool aiPersonaEnabled;
  final int? userId;

  LovedOne({
    required this.id,
    required this.familyId,
    required this.name,
    this.birthDate,
    this.deathDate,
    this.birthPlace,
    this.bio,
    this.isDeceased = true,
    this.aiPersonaEnabled = false,
    this.userId,
  });

  factory LovedOne.fromJson(Map<String, dynamic> json) => LovedOne(
        id: (json['id'] as num).toInt(),
        familyId: (json['familyId'] as num).toInt(),
        name: json['name'] as String,
        birthDate: json['birthDate'] as String?,
        deathDate: json['deathDate'] as String?,
        birthPlace: json['birthPlace'] as String?,
        bio: json['bio'] as String?,
        isDeceased: json['isDeceased'] as bool? ?? true,
        aiPersonaEnabled: json['aiPersonaEnabled'] as bool? ?? false,
        userId: (json['userId'] as num?)?.toInt(),
      );

  LovedOne copyWith({bool? isDeceased, bool? aiPersonaEnabled, int? userId}) => LovedOne(
        id: id,
        familyId: familyId,
        name: name,
        birthDate: birthDate,
        deathDate: deathDate,
        birthPlace: birthPlace,
        bio: bio,
        isDeceased: isDeceased ?? this.isDeceased,
        aiPersonaEnabled: aiPersonaEnabled ?? this.aiPersonaEnabled,
        userId: userId ?? this.userId,
      );
}

class UserProfile {
  final int id;
  final String phone;
  final String name;
  final String? birthDate;

  UserProfile({required this.id, required this.phone, required this.name, this.birthDate});

  factory UserProfile.fromJson(Map<String, dynamic> json) => UserProfile(
        id: (json['id'] as num).toInt(),
        phone: json['phone'] as String? ?? '',
        name: json['name'] as String? ?? '',
        birthDate: json['birthDate'] as String?,
      );
}

class ChatResult {
  final int conversationId;
  final String answer;
  final bool aiFlag;
  final String? usageHint;

  ChatResult({
    required this.conversationId,
    required this.answer,
    required this.aiFlag,
    this.usageHint,
  });

  factory ChatResult.fromJson(Map<String, dynamic> json) => ChatResult(
        conversationId: (json['conversationId'] as num).toInt(),
        answer: json['answer'] as String? ?? '',
        aiFlag: json['aiFlag'] as bool? ?? true,
        usageHint: json['usageHint'] as String?,
      );
}

class MediaItem {
  final int id;
  final int lovedOneId;
  final String mediaType;
  final String? objectKey;
  final int? sizeBytes;
  final String? url;

  MediaItem({
    required this.id,
    required this.lovedOneId,
    required this.mediaType,
    this.objectKey,
    this.sizeBytes,
    this.url,
  });

  factory MediaItem.fromJson(Map<String, dynamic> json) => MediaItem(
        id: (json['id'] as num).toInt(),
        lovedOneId: (json['lovedOneId'] as num).toInt(),
        mediaType: json['mediaType'] as String? ?? '',
        objectKey: json['objectKey'] as String?,
        sizeBytes: (json['sizeBytes'] as num?)?.toInt(),
        url: json['url'] as String?,
      );
}

class FamilyMemberInfo {
  final int userId;
  final String? name;
  final String? phone;
  final String role;

  FamilyMemberInfo({
    required this.userId,
    this.name,
    this.phone,
    required this.role,
  });

  factory FamilyMemberInfo.fromJson(Map<String, dynamic> json) => FamilyMemberInfo(
        userId: (json['userId'] as num).toInt(),
        name: json['name'] as String?,
        phone: json['phone'] as String?,
        role: json['role'] as String? ?? '',
      );
}

class ConsentRecord {
  final int id;
  final int lovedOneId;
  final String consentType;
  final List<int> consentorIds;
  final String? signedAt;
  final String status;

  ConsentRecord({
    required this.id,
    required this.lovedOneId,
    required this.consentType,
    required this.consentorIds,
    this.signedAt,
    required this.status,
  });

  factory ConsentRecord.fromJson(Map<String, dynamic> json) => ConsentRecord(
        id: (json['id'] as num).toInt(),
        lovedOneId: (json['lovedOneId'] as num).toInt(),
        consentType: json['consentType'] as String? ?? '',
        consentorIds: (json['consentorIds'] as List? ?? const [])
            .map((e) => (e as num).toInt())
            .toList(),
        signedAt: json['signedAt'] as String?,
        status: json['status'] as String? ?? '',
      );
}

class InviteKeyInfo {
  final String code;
  final int lovedOneId;
  final String role;
  final String? expiresAt;

  InviteKeyInfo({
    required this.code,
    required this.lovedOneId,
    required this.role,
    this.expiresAt,
  });

  factory InviteKeyInfo.fromJson(Map<String, dynamic> json) => InviteKeyInfo(
        code: json['code'] as String? ?? '',
        lovedOneId: (json['lovedOneId'] as num).toInt(),
        role: json['role'] as String? ?? 'VIEWER',
        expiresAt: json['expiresAt'] as String?,
      );
}

class ClaimResult {
  final int familyId;
  final String role;
  final String relation;
  final String message;

  ClaimResult({
    required this.familyId,
    required this.role,
    required this.relation,
    required this.message,
  });

  factory ClaimResult.fromJson(Map<String, dynamic> json) => ClaimResult(
        familyId: (json['familyId'] as num).toInt(),
        role: json['role'] as String? ?? '',
        relation: json['relation'] as String? ?? '',
        message: json['message'] as String? ?? '',
      );
}
