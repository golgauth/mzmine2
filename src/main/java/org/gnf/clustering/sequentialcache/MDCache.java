package org.gnf.clustering.sequentialcache;

class MDCache
{

    MinDistanceBean first;
    int n;
    int curSize;

    public MDCache(final int initialSize)
    {
        n = initialSize;
    }

    public void insert(final MinDistanceBean newBean) {
        // this.checkExisting(newBean);

        if (first == null) 
        {
            first = newBean;
            return;
        }
        float d = newBean.getDistance();
        if (newBean.getDistance() < first.getDistance()) 
        {
            newBean.setNext(first);
            first = newBean;
            return;
        }
        MinDistanceBean prev = first;
        MinDistanceBean cur = prev.getNext();
        while (cur != null) 
        {
            if (d < cur.distance) 
            {
                break;
            }
            prev = cur;
            cur = cur.getNext();
        }
        this.insertAfter(prev, newBean);
    }

    private void insertAfter(final MinDistanceBean prev, final MinDistanceBean newBean) 
    {
        MinDistanceBean old = prev.getNext();
        prev.setNext(newBean);
        newBean.setNext(old);
        curSize++;
    }

    public MinDistanceBean pop() 
    {
        MinDistanceBean ret = first;
        first = first.getNext();
        ret.setNext(null);
        curSize--;
        return ret;
    }

    void extract(final MinDistanceBean prev, final MinDistanceBean toGo) 
    {
        if (toGo == first) 
        {
            first = toGo.getNext();
        } else 
        {
            prev.setNext(toGo.getNext());
        }
        toGo.setNext(null);
        curSize--;
    }

    private void checkExisting(final MinDistanceBean bean) 
    {
        MinDistanceBean cur = first;
        if (cur == null)
            return;
        while (cur.getNext() != null) 
        {
            if (cur.principal == bean.principal)
                throw new IllegalStateException("Inserting same bean: " + cur.principal + " with remote " + cur.remote + " (existing) and " + bean.remote
                                + " (new)");
            cur = cur.getNext();
        }
    }

    @Override
    public String toString() 
    {
        StringBuilder s = new StringBuilder();
        MinDistanceBean cur = first;
        s.append(cur);
        s.append("\n");
        while (cur.getNext() != null) 
        {
            cur = cur.getNext();
            s.append(cur);
            s.append("\n");
        }
        s.append("n=" + n + "; curSize=" + curSize);
        return s.toString();
    }

}
