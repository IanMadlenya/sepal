package org.openforis.sepal.component.task

interface InstanceProvisioner {
    void provision(Instance instance)

    void reset(Instance instance)
}
