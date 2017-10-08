package org.gnf.clustering.sequentialcache;

class MinDistanceBean
{
	final int principal;
	int remote;
	float distance;

	private MinDistanceBean next;

	public MinDistanceBean(final int principal)
	{
		this.principal = principal;
	}

	public int getRemote()
	{
		return remote;
	}

	public float getDistance()
	{
		return distance;
	}

    public int getPrincipal() 
    {
        return principal;
    }

    public void replace(final int newRemote, final float newDistance)
    {
        remote = newRemote;
        distance = newDistance;
    }

    public MinDistanceBean getNext() 
    {
        return next;
    }

    public void setNext(final MinDistanceBean next) 
    {
        this.next = next;
    }

    @Override
    public String toString() 
    {
        StringBuilder s = new StringBuilder();
        s.append(principal);
        s.append(" -> ");
        s.append(remote);
        s.append(" : ");
        s.append(distance);
        // s.append(Cluster.format.format(distance));
        return s.toString();
    }

}
