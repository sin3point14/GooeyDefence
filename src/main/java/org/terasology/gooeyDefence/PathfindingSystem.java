/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.gooeyDefence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.flexiblepathfinding.PathfinderSystem;
import org.terasology.gooeyDefence.events.OnFieldActivated;
import org.terasology.gooeyDefence.events.OnPathChanged;
import org.terasology.gooeyDefence.events.RepathEnemyRequest;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.world.OnChangedBlock;
import org.terasology.world.block.entity.placement.PlaceBlocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Share(PathfindingSystem.class)
@RegisterSystem
public class PathfindingSystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(PathfindingSystem.class);

    @In
    private PathfinderSystem pathfinderSystem;
    private List<List<Vector3i>> paths = new ArrayList<>(Collections.nCopies(DefenceField.entranceCount(), null));


    /**
     * Called to initialise the field.
     */
    @ReceiveEvent
    public void onFieldActivated(OnFieldActivated event, EntityRef entity) {
        for (int id = 0; id < DefenceField.entranceCount(); id++) {
            event.beginTask();
            calculatePath(id, event::finishTask);
        }
    }


    /**
     * Update path on a block placed
     */
    @ReceiveEvent
    public void onPlaceBlocks(PlaceBlocks event, EntityRef entity) {
        if (DefenceField.isFieldActivated()) {
            calculatePaths();
        }
    }

    /**
     * Update path on a block removed
     */
    @ReceiveEvent
    public void onChangedBlock(OnChangedBlock event, EntityRef entity) {
        if (DefenceField.isFieldActivated()) {
            calculatePaths();
        }
    }

    /**
     * Called to request an enemy be re-pathed. Handles that.
     *
     * @see RepathEnemyRequest
     */
    @ReceiveEvent
    public void onRepathEnemyRequest(RepathEnemyRequest event, EntityRef entity) {
        logger.info("ping");
    }

    /**
     * Calculate the path from an entrance to the centre
     *
     * @param id       The entrance to calculate from
     * @param callback A callback to be invoked after the path calculation has finished.
     */
    private void calculatePath(int id, Runnable callback) {
        calculatePath(DefenceField.entrancePos(id), (path, end) -> {
            List<Vector3i> oldPath = paths.get(id);
            paths.set(id, path);
            if (oldPath != null && !oldPath.equals(path)) {
                DefenceField.getShrineEntity().send(new OnPathChanged(id, path));
            }
            if (callback != null) {
                callback.run();
            }
        });
    }

    private void calculatePath(Vector3i startPoint, BiConsumer<List<Vector3i>, Vector3i> callback) {
        pathfinderSystem.requestPath(DefenceField.fieldCentre(), startPoint, callback::accept);
    }

    /**
     * Calculate paths from all the entrances to the centre.
     */
    private void calculatePaths() {
        for (int id = 0; id < DefenceField.entranceCount(); id++) {
            calculatePath(id, null);
        }
    }

    /**
     * @return All paths from entrance to centre
     */
    public List<List<Vector3i>> getPaths() {
        return paths;
    }

    /**
     * Get a path. Will return null if the path has not been calculated yet.
     *
     * @param pathID Which entrance the path should come from
     * @return The given path, or null if it doesn't exist yet.
     */
    public List<Vector3i> getPath(int pathID) {
        return paths.get(pathID);
    }
}
