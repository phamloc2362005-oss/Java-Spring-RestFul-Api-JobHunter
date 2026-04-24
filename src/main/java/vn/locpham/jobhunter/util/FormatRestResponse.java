package vn.locpham.jobhunter.util;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletResponse;
import vn.locpham.jobhunter.domain.reponse.RestResponse;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;

@ControllerAdvice
public class FormatRestResponse implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // TODO Auto-generated method stub
        // luôn bật format response cho tất cả controller
        // mọi response body đều đi qua beforeBodyWrite
        return true;
    }

    @Override
    public Object beforeBodyWrite(@Nullable Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        // lấy HttpServletResponse để biết status code hiện tại
        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        int status = servletResponse.getStatus();
        // object chuẩn của project để trả về FE
        RestResponse<Object> res = new RestResponse<Object>();
        res.setStatusCode(status);
        // nếu response là String hoặc file Resource thì không bọc lại để tránh lỗi
        // format
        if (body instanceof String || body instanceof Resource) {
            return body;
        }
        // nếu status >= 400 thì thường body lỗi đã được xử lý rồi không bọc thêm nữa
        if (status >= 400) {
            return body;
        } else {
            // với response thành công thì nhét body vào field data
            res.setData(body);
            ApiMessage message = returnType.getMethodAnnotation(ApiMessage.class);
            res.setMessage(message != null ? message.value() : "CALL API SUCCESS");
        }
        return res;
    }

}
