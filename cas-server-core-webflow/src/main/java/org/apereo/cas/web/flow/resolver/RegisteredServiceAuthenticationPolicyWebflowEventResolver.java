package org.apereo.cas.web.flow.resolver;

import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceMultifactorPolicy;
import org.apereo.cas.web.support.WebUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import java.util.Set;

/**
 * This is {@link RegisteredServiceAuthenticationPolicyWebflowEventResolver}
 * that attempts to resolve the next event based on the authentication providers of this service.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@RefreshScope
@Component("registeredServiceAuthenticationPolicyWebflowEventResolver")
public class RegisteredServiceAuthenticationPolicyWebflowEventResolver extends AbstractCasWebflowEventResolver {

    @Override
    protected Set<Event> resolveInternal(final RequestContext context) {
        final RegisteredService service = WebUtils.getRegisteredService(context);
        final Authentication authentication = WebUtils.getAuthentication(context);

        if (service == null || authentication == null) {
            logger.debug("No service or authentication is available to determine event for principal");
            return null;
        }

        final RegisteredServiceMultifactorPolicy policy = service.getMultifactorPolicy();
        if (policy == null || policy.getMultifactorAuthenticationProviders().isEmpty()) {
            logger.debug("Authentication policy does not contain any multifactor authentication providers");
            return null;
        }
        return resolveEventPerAuthenticationProvider(authentication.getPrincipal(), context, service);
    }
}
