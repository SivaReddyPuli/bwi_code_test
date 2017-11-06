package com.core.workflow;

import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

//import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

@Component
@Service
@Properties({ @Property(name = "BWI Project", value = "BWI Activation Workflow"),
		@Property(label = "Workflow Label", name = "process.label", value = "BWI Activation Workflow", description = "This triggers when a page property is modified") })
public class BWIActivationWorkflow implements WorkflowProcess {

	private static final Logger LOG = LoggerFactory.getLogger(BWIActivationWorkflow.class);

	@Reference
	private Replicator replicator;

	@Reference
	private ResourceResolverFactory resolverFactory;

	/**
	 * When activationProperty is not empty activation workflow will be executed
	 * and page will be activated otherwise workflow will be terminated.
	 * 
	 */
	@Override
	public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
			throws WorkflowException {
		LOG.info("Into BWIActivationWorkflow - execute method");
		String path = workItem.getWorkflowData().getPayload().toString();
		ResourceResolver resourceResolver = null;
		// if(StringUtils.isNotBlank(path)) {
		if (!"".equalsIgnoreCase(path)) {
			Session session = workflowSession.adaptTo(Session.class);
			try {
				resourceResolver = getResourceResolver(session);
				if (null != resourceResolver) {

					Resource resource = resourceResolver.getResource(path);
					if (null != resource) {
						getActiavtionProperty(workItem, workflowSession, session, path, resource);

					}
				}
				session.save();
				session.refresh(true);
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				LOG.error("Repository Exception:" + e.getMessage());
			} catch (LoginException e) {
				// TODO Auto-generated catch block
				LOG.error("Login Exception:" + e.getMessage());
			}
		}
	}

	private void getActiavtionProperty(WorkItem workItem, WorkflowSession workflowSession, Session session, String path,
			Resource resource) {
		// TODO Auto-generated method stub

		Node node = resource.adaptTo(Node.class);
		if (null != node) {
			try {
				if (node.hasProperty("activationProperty")) {
					Value activationPropertyValue = node.getProperty("activationProperty").getValue();
					String activationProperty = activationPropertyValue.toString();
					replicatePage(workItem, workflowSession, session, path, activationProperty);

				}
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				LOG.error("Repository Exception:" + e.getMessage());
			}
		}
	}

	private void replicatePage(WorkItem workItem, WorkflowSession workflowSession, Session session, String path,
			String activationProperty) {
		// TODO Auto-generated method stub
		try {
			if (null != activationProperty && activationProperty.length() > 0) {
				replicator.replicate(session, ReplicationActionType.ACTIVATE, path);
				session.save();
				session.refresh(true);

			} else {
				terminateWorkflow(workflowSession, workItem);
			}

		} catch (ReplicationException e) {
			// TODO Auto-generated catch block
			LOG.error("Replication Exception:" + e.getMessage());
		} catch (WorkflowException e) {
			// TODO Auto-generated catch block
			LOG.error("Workflow Exception:" + e.getMessage());
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			LOG.error("Repository Exception:" + e.getMessage());
		}

	}

	/**
	 * Terminate the workflow
	 * 
	 * @param workflowSession
	 *            the {@link WorkflowSession} from the workflow step
	 * @param workItem
	 *            the {@link WorkItem} from the workflow step
	 * @throws WorkflowException
	 *             if there was an error terminating the workflow
	 */
	public static void terminateWorkflow(WorkflowSession workflowSession, WorkItem workItem) throws WorkflowException {
		if (workItem.getWorkflow().isActive()) {
			workflowSession.terminateWorkflow(workItem.getWorkflow());
		}
	}

	/*
	 * Gets a ResourceResolver from the factory with the given Session
	 */
	public ResourceResolver getResourceResolver(Session session) throws LoginException {
		return resolverFactory.getResourceResolver(
				Collections.<String, Object>singletonMap(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session));
	}

}
