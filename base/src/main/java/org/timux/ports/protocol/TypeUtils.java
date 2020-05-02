package org.timux.ports.protocol;

import org.timux.ports.*;

class TypeUtils {

    public static void verifyResponseType(Class<?> requestType, Class<?> responseType) {
        verifyResponseType(requestType, responseType, null, null, null);
    }

    public static void verifyResponseType(
            Class<?> requestType,
            Class<?> responseType,
            Class<?> eitherTypeA,
            Class<?> eitherTypeB)
    {
        verifyResponseType(requestType, responseType, eitherTypeA, eitherTypeB, null);
    }

    public static void verifyResponseType(
            Class<?> messageType,
            Class<?> responseType,
            Class<?> eitherTypeA,
            Class<?> eitherTypeB,
            Class<?> eitherTypeC)
    {
        if (messageType.getName().endsWith("Event") || messageType.getName().endsWith("Exception")) {
            if (responseType != void.class || eitherTypeA != null || eitherTypeB != null || eitherTypeC != null) {
                throw new EventTypeResponseException(messageType.getName());
            }

            return;
        }

        Response responseAnno = messageType.getDeclaredAnnotation(Response.class);
        Responses responsesAnno = messageType.getDeclaredAnnotation(Responses.class);
        SuccessResponse successResponseAnno = messageType.getDeclaredAnnotation(SuccessResponse.class);
        FailureResponse failureResponseAnno = messageType.getDeclaredAnnotation(FailureResponse.class);

        String declaredResponseType = null;

        if (responseAnno != null) {
            declaredResponseType = responseAnno.value().getName();
        }

        if (responsesAnno != null) {
            if (responsesAnno.value().length < 2 || responsesAnno.value().length > 3) {
                throw new InvalidResponseDeclarationException(messageType.getName());
            }

            if (responsesAnno.value().length == 2 && eitherTypeB == null) {
                throw new InsufficientResponseTypesException(messageType.getName(), 2, 1);
            }

            if (responsesAnno.value().length == 3 && eitherTypeC == null) {
                throw new InsufficientResponseTypesException(messageType.getName(), 3, eitherTypeB == null ? 1 : 2);
            }

            if (eitherTypeA != null && eitherTypeB != null) {
                if (responsesAnno.value()[0].value() != eitherTypeA) {
                    throw new InvalidResponseTypeException(eitherTypeA.getName(), messageType.getName());
                }

                if (responsesAnno.value()[1].value() != eitherTypeB) {
                    throw new InvalidResponseTypeException(eitherTypeB.getName(), messageType.getName());
                }
            } else if (responseType == Either.class) {
                throw new RawUnionTypeException(
                        messageType.getName(),
                        responsesAnno.value()[0].value().getName(),
                        responsesAnno.value()[1].value().getName());
            }

            declaredResponseType = responsesAnno.value().length == 2
                    ? Either.class.getName()
                    : Either3.class.getName();
        }

        if (successResponseAnno == null ^ failureResponseAnno == null) {
            throw new InvalidResponseDeclarationException(responseType.getName());
        }

        if (successResponseAnno != null) {
            if (eitherTypeA != null && eitherTypeB != null) {
                if (successResponseAnno.value() != eitherTypeA) {
                    throw new InvalidResponseTypeException(eitherTypeA.getName(), messageType.getName());
                }

                if (failureResponseAnno.value() != eitherTypeB) {
                    throw new InvalidResponseTypeException(eitherTypeB.getName(), messageType.getName());
                }
            } else if (responseType == Either.class) {
                throw new RawUnionTypeException(
                        messageType.getName(),
                        successResponseAnno.value().getName(),
                        failureResponseAnno.value().getName());
            }

            declaredResponseType = Either.class.getName();
        }

        if (declaredResponseType == null) {
            System.err.println("[ports] warning: no response type declaration provided by request type " + messageType.getName());
            return;
        }

        if (!declaredResponseType.equals(responseType.getName())) {
            throw new ResponseTypesDoNotMatchException(messageType.getName(), declaredResponseType, responseType.getName());
        }
    }
}
