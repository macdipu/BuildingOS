/// Stable failure codes the presentation layer localizes: API `ApiError.code` values plus client-side ones.
abstract final class ApplicationErrorCode {
  static const incomplete = 'APPLICATION_INCOMPLETE';
  static const notEditable = 'NOT_EDITABLE';
  static const invalidTransition = 'INVALID_TRANSITION';
  static const notFound = 'APPLICATION_NOT_FOUND';
  static const documentNotFound = 'DOCUMENT_NOT_FOUND';
  static const documentTooLarge = 'DOCUMENT_TOO_LARGE';
  static const unsupportedDocument = 'UNSUPPORTED_DOCUMENT_TYPE';
  static const documentLimit = 'DOCUMENT_LIMIT_REACHED';
  static const invalidRequest = 'INVALID_REQUEST';
  static const unavailable = 'DEPENDENCY_UNAVAILABLE';
  static const connection = 'CONNECTION';
  static const unexpected = 'UNEXPECTED';
}
