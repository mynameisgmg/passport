package org.apereo.cas.config;

import com.google.common.collect.ImmutableList;
import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.audit.spi.AssertionAsReturnValuePrincipalResolver;
import org.apereo.cas.audit.spi.CredentialsAsFirstParameterResourceResolver;
import org.apereo.cas.audit.spi.ServiceResourceResolver;
import org.apereo.cas.audit.spi.TicketOrCredentialPrincipalResolver;
import org.apereo.inspektr.audit.AuditTrailManagementAspect;
import org.apereo.inspektr.audit.AuditTrailManager;
import org.apereo.inspektr.audit.spi.AuditActionResolver;
import org.apereo.inspektr.audit.spi.AuditResourceResolver;
import org.apereo.inspektr.audit.spi.support.DefaultAuditActionResolver;
import org.apereo.inspektr.audit.spi.support.ReturnValueAsStringResourceResolver;
import org.apereo.inspektr.audit.support.AbstractStringAuditTrailManager;
import org.apereo.inspektr.audit.support.Slf4jLoggingAuditTrailManager;
import org.apereo.inspektr.common.spi.PrincipalResolver;
import org.apereo.inspektr.common.web.ClientInfoThreadLocalFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This is {@link CasAuditTrailConfiguration} that configures relevant audit trail beans.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration("casAuditTrailConfiguration")
@EnableAspectJAutoProxy
public class CasAuditTrailConfiguration {
    private static final String AUDIT_ACTION_SUFFIX_FAILED = "_FAILED";

    @Value("${cas.audit.appcode:CAS}")
    private String appCode;

    @Autowired
    @Qualifier("ticketResourceResolver")
    private AuditResourceResolver ticketResourceResolver;

    @Autowired
    @Qualifier("messageBundleAwareResourceResolver")
    private AuditResourceResolver messageBundleAwareResourceResolver;

    @Value("${cas.audit.singleline.separator:|}")
    private String entrySeparator;

    @Value("${cas.audit.singleline:false}")
    private boolean useSingleLine;

    @Value("${cas.audit.format:DEFAULT}")
    private AbstractStringAuditTrailManager.AuditFormats auditFormat;

    @Value("${cas.audit.ignore.failures:true}")
    private boolean ignoreAuditFailures;
    
    /**
     * Audit trail management aspect audit trail management aspect.
     *
     * @param centralAuthenticationService centralAuthenticationService
     * @return the audit trail management aspect
     */
    @Autowired
    @Bean(name = "auditTrailManagementAspect")
    public AuditTrailManagementAspect auditTrailManagementAspect(final CentralAuthenticationService centralAuthenticationService) {
        final AuditTrailManagementAspect aspect = new AuditTrailManagementAspect(this.appCode,
                auditablePrincipalResolver(centralAuthenticationService), ImmutableList.of(auditTrailManager()), auditActionResolverMap(),
                auditResourceResolverMap());
        aspect.setFailOnAuditFailures(!this.ignoreAuditFailures);
        return aspect;
    }

    /**
     * Audit trail manager audit trail manager.
     *
     * @return the audit trail manager
     */
    @Bean(name = "slf4jAuditTrailManager")
    public AuditTrailManager auditTrailManager() {
        final Slf4jLoggingAuditTrailManager mgmr = new Slf4jLoggingAuditTrailManager();
        mgmr.setUseSingleLine(this.useSingleLine);
        mgmr.setEntrySeparator(this.entrySeparator);
        mgmr.setAuditFormat(this.auditFormat);
        return mgmr;
    }


    /**
     * Cas client info logging filter client info thread local filter.
     *
     * @return the client info thread local filter
     */
    @Bean(name = "casClientInfoLoggingFilter")
    public FilterRegistrationBean casClientInfoLoggingFilter() {
        final FilterRegistrationBean bean = new FilterRegistrationBean();
        bean.setFilter(new ClientInfoThreadLocalFilter());
        bean.setUrlPatterns(Collections.singleton("/*"));
        bean.setName("CAS Client Info Logging Filter");
        return bean;
    }

    /**
     * Authentication action resolver default audit action resolver.
     *
     * @return the default audit action resolver
     */
    @Bean(name = "authenticationActionResolver")
    public DefaultAuditActionResolver authenticationActionResolver() {
        return new DefaultAuditActionResolver("_SUCCESS", AUDIT_ACTION_SUFFIX_FAILED);
    }

    /**
     * Ticket creation action resolver default audit action resolver.
     *
     * @return the default audit action resolver
     */
    @Bean(name = "ticketCreationActionResolver")
    public DefaultAuditActionResolver ticketCreationActionResolver() {
        return new DefaultAuditActionResolver("_CREATED", "_NOT_CREATED");
    }

    /**
     * Ticket validation action resolver default audit action resolver.
     *
     * @return the default audit action resolver
     */
    @Bean(name = "ticketValidationActionResolver")
    public DefaultAuditActionResolver ticketValidationActionResolver() {
        return new DefaultAuditActionResolver("D", AUDIT_ACTION_SUFFIX_FAILED);
    }

    /**
     * Return value resource resolver return value as string resource resolver.
     *
     * @return the return value as string resource resolver
     */
    @Bean(name = "returnValueResourceResolver")
    public ReturnValueAsStringResourceResolver returnValueResourceResolver() {
        return new ReturnValueAsStringResourceResolver();
    }


    /**
     * Audit action resolver map map.
     *
     * @return the map
     */
    @Bean(name = "auditActionResolverMap")
    public Map auditActionResolverMap() {
        final Map<String, AuditActionResolver> map = new HashMap<>();
        map.put("AUTHENTICATION_RESOLVER", authenticationActionResolver());
        map.put("SAVE_SERVICE_ACTION_RESOLVER", authenticationActionResolver());
        map.put("CREATE_TICKET_GRANTING_TICKET_RESOLVER", ticketCreationActionResolver());
        map.put("DESTROY_TICKET_GRANTING_TICKET_RESOLVER", new DefaultAuditActionResolver());
        map.put("CREATE_PROXY_GRANTING_TICKET_RESOLVER", ticketCreationActionResolver());
        map.put("DESTROY_PROXY_GRANTING_TICKET_RESOLVER", new DefaultAuditActionResolver());
        map.put("GRANT_SERVICE_TICKET_RESOLVER", ticketCreationActionResolver());
        map.put("GRANT_PROXY_TICKET_RESOLVER", ticketCreationActionResolver());
        map.put("VALIDATE_SERVICE_TICKET_RESOLVER", ticketValidationActionResolver());
        return map;
    }

    /**
     * Audit resource resolver map map.
     *
     * @return the map
     */
    @Bean(name = "auditResourceResolverMap")
    public Map auditResourceResolverMap() {
        final Map<String, AuditResourceResolver> map = new HashMap<>();
        map.put("AUTHENTICATION_RESOURCE_RESOLVER", new CredentialsAsFirstParameterResourceResolver());
        map.put("CREATE_TICKET_GRANTING_TICKET_RESOURCE_RESOLVER", this.messageBundleAwareResourceResolver);
        map.put("CREATE_PROXY_GRANTING_TICKET_RESOURCE_RESOLVER", this.messageBundleAwareResourceResolver);
        map.put("DESTROY_TICKET_GRANTING_TICKET_RESOURCE_RESOLVER", this.ticketResourceResolver);
        map.put("DESTROY_PROXY_GRANTING_TICKET_RESOURCE_RESOLVER", this.ticketResourceResolver);
        map.put("GRANT_SERVICE_TICKET_RESOURCE_RESOLVER", new ServiceResourceResolver());
        map.put("GRANT_PROXY_TICKET_RESOURCE_RESOLVER", new ServiceResourceResolver());
        map.put("VALIDATE_SERVICE_TICKET_RESOURCE_RESOLVER", this.ticketResourceResolver);
        map.put("SAVE_SERVICE_RESOURCE_RESOLVER", returnValueResourceResolver());
        return map;
    }

    /**
     * Principal resolver.
     *
     * @param centralAuthenticationService centralAuthenticationService
     * @return the principal resolver
     */
    @Bean(name = "auditablePrincipalResolver")
    public PrincipalResolver auditablePrincipalResolver(final CentralAuthenticationService centralAuthenticationService) {
        return new AssertionAsReturnValuePrincipalResolver(new TicketOrCredentialPrincipalResolver(centralAuthenticationService));
    }
}

