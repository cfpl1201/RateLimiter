package com.dbq.filter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;


/**
 * xss过滤器
 *
 * @author dbq
 * @date 2020/7/13 17:00
 */
@Component
@ConfigurationProperties("em.gateway.xss-filter")
public class XssRequestGlobalFilter extends BaseGlobalFilter implements GlobalFilter, Ordered {
	private Logger log = LoggerFactory.getLogger(XssRequestGlobalFilter.class);
	private List<String> excludePaths;

	public void setExcludePaths(List<String> excludePaths) {
		this.excludePaths = excludePaths;
	}

	/**
	 * @param exchange
	 * @param chain
	 * @return get请求参考spring cloud gateway自带过滤器：
	 * @see org.springframework.cloud.gateway.filter.factory.AddRequestParameterGatewayFilterFactory
	 * <p>
	 * post请求参考spring cloud gateway自带过滤器：
	 * @see org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest serverHttpRequest = exchange.getRequest();
		HttpMethod method = serverHttpRequest.getMethod();
		String contentType = serverHttpRequest.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

		Boolean postFlag = (method == HttpMethod.POST || method == HttpMethod.PUT) &&
				(MediaType.APPLICATION_FORM_URLENCODED_VALUE.equalsIgnoreCase(contentType) || MediaType.APPLICATION_JSON_VALUE.equals(contentType) || MediaType.APPLICATION_JSON_UTF8_VALUE.equals(contentType));

		// get 请求， 参考的是 org.springframework.cloud.gateway.filter.factory.AddRequestParameterGatewayFilterFactory
		if (method == HttpMethod.GET) {
			URI uri = exchange.getRequest().getURI();

			String rawQuery = uri.getRawQuery();
			if (StringUtils.isBlank(rawQuery)) {
				return chain.filter(exchange);
			}
			rawQuery = XssCleanRuleUtils.xssClean(rawQuery);
			try {
				URI newUri = UriComponentsBuilder.fromUri(uri)
						.replaceQuery(rawQuery)
						.build(false)
						.toUri();

				ServerHttpRequest request = exchange.getRequest().mutate().uri(newUri).build();
				return chain.filter(exchange.mutate().request(request).build());
			} catch (Exception e) {
				log.error("get请求清理xss攻击异常", e);
				throw new IllegalStateException("Invalid URI query: \"" + rawQuery + "\"");
			}
		}
		//post请求时，如果是文件上传之类的请求，不修改请求消息体
		else if (postFlag) {
			// 参考的是 org.springframework.cloud.gateway.filter.factory.AddRequestParameterGatewayFilterFactory

			//从请求里获取Post请求体
			String bodyStr = resolveBodyFromRequest(serverHttpRequest);
			if (StringUtils.isEmpty(bodyStr)) {
				bodyStr = "";
			}
			bodyStr = XssCleanRuleUtils.xssClean(bodyStr);

			return rebuild(exchange, chain, bodyStr);
		} else {
			return chain.filter(exchange);
		}

	}

	@Override
	public int getOrder() {
		return 1;
	}
}
