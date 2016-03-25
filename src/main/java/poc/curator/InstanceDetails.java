package poc.curator;

import org.codehaus.jackson.map.annotate.JsonRootName;

/**
 * Depicts the Details for our Service Instance. These are details that we want to expose for other services to discover.
 * This should not include any IP or Port details since that is already covered.
 *
 * Note the JSON annotation: If you view the details on the server, the json form will be visible.
 *
 * Taken from Curator examples.
 */
@JsonRootName("servicedetails")
public class InstanceDetails {

  private String description;

  public InstanceDetails() {
    this("");
  }

  public InstanceDetails(String description) {
    this.description = description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
