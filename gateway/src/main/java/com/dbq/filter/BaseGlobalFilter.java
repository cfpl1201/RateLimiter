package com.dbq.filter;

import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基础过滤器
 *
 * @author dbq
 * @date 2020/8/4 10:48
 */
public class BaseGlobalFilter {

	public Mono<Void> rebuild(ServerWebExchange exchange,
	                          GatewayFilterChain chain,
	                          String bodyStr){
		ServerHttpRequest serverHttpRequest = exchange.getRequest();
		String contentType = serverHttpRequest.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
		URI uri = serverHttpRequest.getURI();
		URI newUri = UriComponentsBuilder.fromUri(uri).build(false).toUri();
		ServerHttpRequest request = exchange.getRequest().mutate().uri(newUri).build();
		DataBuffer bodyDataBuffer = stringBuffer(bodyStr);
		Flux<DataBuffer> bodyFlux = Flux.just(bodyDataBuffer);

		// 定义新的消息头
		HttpHeaders headers = new HttpHeaders();
		headers.putAll(exchange.getRequest().getHeaders());


		// 由于修改了传递参数，需要重新设置CONTENT_LENGTH，长度是字节长度，不是字符串长度
		int length = bodyStr.getBytes().length;
		headers.remove(HttpHeaders.CONTENT_LENGTH);
		headers.setContentLength(length);

		// 设置CONTENT_TYPE
		if (StringUtils.isNotBlank(contentType)) {
			headers.set(HttpHeaders.CONTENT_TYPE, contentType);
		}

		// 由于post的body只能订阅一次，由于上面代码中已经订阅过一次body。所以要再次封装请求到request才行，不然会报错请求已经订阅过
		request = new ServerHttpRequestDecorator(request) {
			@Override
			public HttpHeaders getHeaders() {
				long contentLength = headers.getContentLength();
				HttpHeaders httpHeaders = new HttpHeaders();
				httpHeaders.putAll(super.getHeaders());
				if (contentLength > 0) {
					httpHeaders.setContentLength(contentLength);
				} else {
					// this causes a 'HTTP/1.1 411 Length Required' on httpbin.org
					httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
				}
				return httpHeaders;
			}

			@Override
			public Flux<DataBuffer> getBody() {
				return bodyFlux;
			}
		};

		//封装request，传给下一级
		request.mutate().header(HttpHeaders.CONTENT_LENGTH, Integer.toString(bodyStr.length()));
		return chain.filter(exchange.mutate().request(request).build());
	}
	/**
	 * 从Flux<DataBuffer>中获取字符串的方法
	 *
	 * @return 请求体
	 */
	public String resolveBodyFromRequest(ServerHttpRequest serverHttpRequest) {
		//获取请求体
		Flux<DataBuffer> body = serverHttpRequest.getBody();
		AtomicReference<String> bodyRef = new AtomicReference<>();
		body.subscribe(buffer -> {
			CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer.asByteBuffer());
			DataBufferUtils.release(buffer);
			bodyRef.set(charBuffer.toString());
		});
		//获取request body
		return bodyRef.get();
	}

	/**
	 * 字符串转DataBuffer
	 *
	 * @param value
	 * @return
	 */
	private DataBuffer stringBuffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
		DataBuffer buffer = nettyDataBufferFactory.allocateBuffer(bytes.length);
		buffer.write(bytes);
		return buffer;
	}
}
