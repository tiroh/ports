package org.timux.ports.testapp.component;

import org.timux.ports.FailureResponse;
import org.timux.ports.SuccessResponse;

@SuccessResponse(Integer.class)
@FailureResponse(String.class)
public class FragileRequest {
}
