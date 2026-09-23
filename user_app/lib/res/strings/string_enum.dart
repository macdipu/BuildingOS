import 'package:get/get.dart';

enum TextEnum {
  appName(en: "App Name", bn: "অ্যাপ নাম"),
  verify(en: "Verify", bn: "যাচাই করুন"),
  send(en: "Send", bn: "পাঠান"),
  submit(en: "Submit", bn: "জমা দিন"),
  ok(en: "OK", bn: "ঠিক আছে"),
  cancel(en: "Cancel", bn: "বাতিল করুন"),
  resend(en: "Resend", bn: "পুনরায় পাঠান"),
  about(en: "About", bn: "সম্পর্কে"),
  contactUs(en: "Contact Us", bn: "যোগাযোগ করুন"),
  loginDescription(
      en: "Enter your credentials to login", bn: "লগইন করতে আপনার তথ্য দিন"),
  loginUpperText(en: "Login to continue", bn: "চালিয়ে যাওয়ার জন্য লগইন করুন"),
  next(en: "Next", bn: "পরবর্তী"),
  otpResend(
      en: "OTP will be sent again in @time", bn: '@time এবার OTP পাঠাবেন'),

  otpVerification(en: "OTP Verification", bn: "OTP যাচাইকরণ"),
  otpSentTo(
      en: "Please check, a verification code has been sent to @phone",
      bn: "@phone নম্বরে একটি যাচাইকরণ কোড পাঠানো হয়েছে"),
  changeNumber(en: "Change Number", bn: "নম্বর পরিবর্তন করুন"),

  phoneNumber(en: "Phone Number", bn: "ফোন নম্বর"),
  pin(en: "PIN", bn: "পিন"),
  createAccount(en: "Create new account", bn: "নতুন অ্যাকাউন্ট তৈরি করুন"),
  // Add more entries as required
  ;

  final String en;
  final String bn;

  const TextEnum({required this.en, required this.bn});
}

extension TextEnumExtention on TextEnum {
  String get _key => name;
  String get tr => _key.tr;
  String trParams([Map<String, String>? params]) => _key.trParams(params ?? {});
}
