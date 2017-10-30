/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
@Properties({
	@Property(
            name = "BWI Project",
            value = "BWI Activation Workflow"
    ),
    @Property(
            label = "Workflow Label",
            name = "process.label",
            value = "BWI Activation Workflow",
            description = "This triggers when a page property is modified"
    )
})
public class BWIActivationWorkflow implements WorkflowProcess{
	
	private static final Logger LOG = LoggerFactory.getLogger(BWIActivationWorkflow.class);
		
	@Reference
	private Replicator replicator;
	
	@Reference
    private ResourceResolverFactory resolverFactory;
	
	/**
	 * When activationProperty is not empty activation workflow will be executed and page will be activated otherwise workflow will be terminated.
	 * 
	 */
	@Override
	public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
			throws WorkflowException {
		LOG.info("Into BWIActivationWorkflow - execute method");
		String path = workItem.getWorkflowData().getPayload().toString();
		ResourceResolver resourceResolver = null;
		//if(StringUtils.isNotBlank(path)) {
		if(!"".equalsIgnoreCase(path)) {
			Session session = workflowSession.adaptTo(Session.class);
			try {
				resourceResolver = getResourceResolver(session);	
				if(null != resourceResolver) {
					
					Resource resource = resourceResolver.getResource(path);
					if(null != resource) {
						
						Node node = resource.adaptTo(Node.class);
						if(null != node) {
							if(node.hasProperty("activationProperty")) {
								Value activationPropertyValue = node.getProperty("activationProperty").getValue();
								
								String activationProperty = activationPropertyValue.toString();
								
								
								if(null != activationProperty && activationProperty.length() > 0) {										
									replicator.replicate(session, ReplicationActionType.ACTIVATE,
											path);										
									session.save();
									session.refresh(true);
									
								}else {
									terminateWorkflow(workflowSession, workItem);
								}
							}						
						}
						
					}
				}
				session.save();
				session.refresh(true);
			}catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch (ReplicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch (LoginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
	
	
	/**
	 * Terminate the workflow
	 * 
	 * @param workflowSession the {@link WorkflowSession} from the workflow step
	 * @param workItem the {@link WorkItem} from the workflow step
	 * @throws WorkflowException if there was an error terminating the workflow
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
		return resolverFactory.getResourceResolver(Collections.<String, Object>singletonMap(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session));
	}

}
