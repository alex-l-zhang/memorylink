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

  LovedOne({
    required this.id,
    required this.familyId,
    required this.name,
    this.birthDate,
    this.deathDate,
    this.birthPlace,
    this.bio,
  });

  factory LovedOne.fromJson(Map<String, dynamic> json) => LovedOne(
        id: (json['id'] as num).toInt(),
        familyId: (json['familyId'] as num).toInt(),
        name: json['name'] as String,
        birthDate: json['birthDate'] as String?,
        deathDate: json['deathDate'] as String?,
        birthPlace: json['birthPlace'] as String?,
        bio: json['bio'] as String?,
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
