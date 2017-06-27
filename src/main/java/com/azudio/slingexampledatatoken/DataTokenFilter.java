package com.azudio.slingexampledatatoken;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Simple Filter
 *
 * Annotations below are short version of:
 *
 * @Component @Service(Filter.class) @Properties({ @Property(name="service.description",
 *            value="A Simple Filter"), @Property(name="service.vendor",
 *            value="The Apache Software
 *            Foundation"), @Property(name="sling.filter.scope",
 *            value="REQUEST"), @Property(name="service.ranking", intValue=1) })
 */
@SlingFilter(order = 1, description = "A DataToken Filter")
@Properties({ @Property(name = "service.vendor", value = "Adam Henderson"), @Property(name = "sling.filter.pattern", value = ".*/content/.*"), })
public class DataTokenFilter implements Filter {

    private static final String HEADER_CONTAINS_TOKENISED_CONTENT = "x-ctc";

    public void activate(final BundleContext context) {
        log.info("Activated " + context.getClass().getName());
    }

    private final Logger log = LoggerFactory.getLogger(DataTokenFilter.class);

    // Should really be injected in by OSGI
    private TokenService tokenService;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

        PrintWriter out = response.getWriter();
        CharResponseWrapper wrapper = new CharResponseWrapper((HttpServletResponse) response);

        // Invoke the chain
        chain.doFilter(request, wrapper);

        if ((response.getContentType() != null) && response.getContentType().contains("text/html")) {
            CharArrayWriter caw = new CharArrayWriter();

            String markup = wrapper.toString();

            String resolved = replaceTokens(markup, request, wrapper, tokenService);

            caw.write(resolved);

            ((HttpServletResponse) response).setHeader(HEADER_CONTAINS_TOKENISED_CONTENT, "true");

            response.setContentLength(caw.toString().getBytes().length);

            out.write(caw.toString());

        }
    }

    private String replaceTokens(final String markup, final ServletRequest request, final CharResponseWrapper wrapper, final TokenService tokenService) {

        // TODO: Implement loking for tokens in markup and replacing with values
        // from the TokenService

        // 1. Find all 'token' strings in markup
        // 2. Iterate through each found token and call implementation through
        // look up in TokenService replacingAll
        // 3. Return final markup

        // Text might contain a token with params:
        // Hello ${users-name,tone=formal}.

        // Params are a String[] of k=v tuple strings
        // tokenService.getToken("datatoken1", "key1=val1");

        return markup.replaceAll("\\$\\{datatoken1\\}", tokenService.getToken("cowboy-greeting"));
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        tokenService = new TokenService();
        tokenService.registerToken(new CowboyGreetingToken());
    }

    @Override
    public void destroy() {
        // n/a
    }

    private class CowboyGreetingToken implements Token {

        private final String NAME = "cowboy-greeting";
        // private String[] parameters;

        @Override
        public String getTokenName() {
            return NAME;
        }

        @Override
        public String getValue(final String... params) {
            return "Hello Cowboy! Yee-Haaa!";
        }

    }

    /**
     * Represents a Token
     * <p>
     * Idea: Could a Token be represented as a node in the JCR which this
     * service queries for (at activation???) ? With node name + properties
     * like: MyToken={implemenatationclass=com.example.MyToken, active=true},
     * each Token implemented would be an OSGI Service, only 'active' would be
     * loaded
     *
     * @author adamhenderson
     *
     */
    private interface Token {

        public String getTokenName();

        public String getValue(String... params);
    }

    /**
     * A service that manages a list of active tokens
     *
     * @author adamhenderson
     *
     */
    private class TokenService {

        private final Map<String, Token> map = new HashMap<>();

        public void registerToken(final Token token) {

            if (map.containsKey(token.getTokenName())) {
                log.info(String.format("%s could not be registered as a token with that name already exists", token.getTokenName()));
                return;
            }

            map.put(token.getTokenName(), token);
        }

        /**
         * @param tokenName
         * @param params
         * @return
         */
        public String getToken(final String tokenName, final String... params) {

            if (map.containsKey(tokenName)) {
                return map.get(tokenName).getValue(params);
            }

            return "";
        }

    }

}
